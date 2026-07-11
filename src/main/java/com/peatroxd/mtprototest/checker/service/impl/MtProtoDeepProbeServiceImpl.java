package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.MtProtoDeepProbeResult;
import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.checker.model.ProxySecretDetails;
import com.peatroxd.mtprototest.checker.model.ProxySecretType;
import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import com.peatroxd.mtprototest.checker.service.MtProtoDeepProbeService;
import com.peatroxd.mtprototest.checker.service.ProxySecretParser;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class MtProtoDeepProbeServiceImpl implements MtProtoDeepProbeService {

    private static final int CONNECT_TIMEOUT_MS = 2_500;
    private static final int MAX_PACKET_LENGTH = 4_096;
    private static final int MT_PROXY_DC_ID = 2;
    private static final byte PADDED_INTERMEDIATE_PREFIX = (byte) 0xDD;
    private static final int RES_PQ_CONSTRUCTOR_ID = 0x05162463;
    private static final int REQ_PQ_MULTI_CONSTRUCTOR_ID = 0xBE7E8EF1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int POST_HANDSHAKE_HOLD_MS = 1500;

    private final ProxySecretParser proxySecretParser;
    private final ProbeSocketFactory socketFactory;
    private final int readTimeoutMs;

    public MtProtoDeepProbeServiceImpl(ProxySecretParser proxySecretParser,
                                       ProbeSocketFactory socketFactory,
                                       CheckerProperties checkerProperties) {
        this.proxySecretParser = proxySecretParser;
        this.socketFactory = socketFactory;
        this.readTimeoutMs = checkerProperties.getReadTimeoutMs();
    }

    @Override
    public MtProtoDeepProbeResult probe(ProxyEntity proxy) {
        ProxySecretDetails secretDetails = proxySecretParser.parse(proxy.getSecret());

        if (secretDetails.type() == ProxySecretType.FAKE_TLS) {
            return probeFakeTls(proxy, secretDetails);
        }

        if (!secretDetails.supported()) {
            MtProtoProbeFailureCode failureCode = secretDetails.type() == ProxySecretType.FAKE_TLS
                    ? MtProtoProbeFailureCode.UNSUPPORTED_SECRET_FORMAT
                    : MtProtoProbeFailureCode.INVALID_SECRET;
            return MtProtoDeepProbeResult.failure(failureCode, secretDetails.message());
        }

        Instant startedAt = Instant.now();

        try (Socket socket = socketFactory.create()) {
            socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(readTimeoutMs);

            CryptoSession session = openObfuscatedSession(socket, secretDetails.keyBytes());
            byte[] nonce = randomBytes(16);

            writeFully(socket.getOutputStream(), session.encrypt(buildProbeRequest(nonce)));

            byte[] encryptedLength = readFully(socket.getInputStream(), 4);
            byte[] decryptedLength = session.decrypt(encryptedLength);
            int totalLength = littleEndianInt(decryptedLength, 0);

            if (totalLength <= 0 || totalLength > MAX_PACKET_LENGTH) {
                return MtProtoDeepProbeResult.failure(
                        MtProtoProbeFailureCode.INVALID_RESPONSE,
                        "Unexpected packet length: " + totalLength
                );
            }

            byte[] encryptedPayload = readFully(socket.getInputStream(), totalLength);
            byte[] payload = session.decrypt(encryptedPayload);

            if (isTransportError(payload)) {
                int transportErrorCode = Math.abs(littleEndianInt(payload, 0));
                return MtProtoDeepProbeResult.failure(
                        MtProtoProbeFailureCode.TRANSPORT_ERROR,
                        "MTProto transport error: " + transportErrorCode
                );
            }

            if (!containsResPq(payload, nonce)) {
                return MtProtoDeepProbeResult.failure(
                        MtProtoProbeFailureCode.RES_PQ_NOT_RECEIVED,
                        "resPQ was not received from proxy"
                );
            }

            try {
                socket.setSoTimeout(POST_HANDSHAKE_HOLD_MS);   // напр. 1500 мс
                int next = socket.getInputStream().read();
                if (next == -1) {
                    // сервер/ТСПУ закрыл соединение после resPQ — для юзера прокси НЕ работает
                    return MtProtoDeepProbeResult.failure(
                            MtProtoProbeFailureCode.TRANSPORT_ERROR,
                            "Connection closed after resPQ (likely DPI reset)");
                }
                // пришли ещё байты — соединение живёт, DPI не оборвал; это хороший знак
            } catch (SocketTimeoutException e) {
                // таймаут БЕЗ разрыва = соединение держится, сервер просто молчит — это ОК (жив)
            } catch (IOException e) {
                // RST/сетевой обрыв в окне досмотра — трактуем как блокировку
                return MtProtoDeepProbeResult.failure(
                        MtProtoProbeFailureCode.TRANSPORT_ERROR,
                        "IO error after resPQ (likely DPI reset): " + e.getMessage());
            }

            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            return MtProtoDeepProbeResult.success(latencyMs);
        } catch (EOFException e) {
            return MtProtoDeepProbeResult.failure(MtProtoProbeFailureCode.INVALID_RESPONSE, "Unexpected end of stream");
        } catch (Exception e) {
            MtProtoProbeFailureCode failureCode = e instanceof java.net.SocketTimeoutException
                    ? MtProtoProbeFailureCode.IO_ERROR
                    : MtProtoProbeFailureCode.CONNECT_ERROR;

            log.debug("Deep MTProto probe failed for proxyId={}: {}", proxy.getId(), e.getMessage(), e);
            return MtProtoDeepProbeResult.failure(failureCode, e.getMessage());
        }
    }

    private MtProtoDeepProbeResult probeFakeTls(ProxyEntity proxy, ProxySecretDetails secretDetails) {
        byte[] keyAndDomain = secretDetails.keyBytes();
        if (keyAndDomain == null || keyAndDomain.length < 17) {
            return MtProtoDeepProbeResult.failure(
                    MtProtoProbeFailureCode.INVALID_SECRET, "ee-secret too short");
        }
        byte[] key = slice(keyAndDomain, 0, 16);
        String domain = new String(slice(keyAndDomain, 16, keyAndDomain.length), StandardCharsets.UTF_8);

        Instant startedAt = Instant.now();
        try (Socket socket = socketFactory.create()) {
            socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(readTimeoutMs);

            writeFully(socket.getOutputStream(), buildFakeTlsClientHello(key, domain));

            byte[] header = readFully(socket.getInputStream(), 5);
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();

            int recordType = header[0] & 0xFF;
            int recordLen = ((header[3] & 0xFF) << 8) | (header[4] & 0xFF);

            log.debug("FakeTLS proxyId={} host={} -> type=0x{} len={} latency={}ms",
                    proxy.getId(), proxy.getHost(),
                    Integer.toHexString(recordType), recordLen, latencyMs);

            // Live fake-TLS MTProxy answers with a TLS handshake record (ServerHello). Wrong HMAC ->
            // proxy fronts to the real site, so we'd see something other than a plausible handshake.
            if (recordType != 0x16) {
                return MtProtoDeepProbeResult.failure(MtProtoProbeFailureCode.INVALID_RESPONSE,
                        "Not TLS handshake: type=0x" + Integer.toHexString(recordType));
            }
            if (recordLen <= 0 || recordLen > 16384) {
                return MtProtoDeepProbeResult.failure(MtProtoProbeFailureCode.INVALID_RESPONSE,
                        "Implausible ServerHello length: " + recordLen);
            }

            readFully(socket.getInputStream(), recordLen); // drain ServerHello body
            // ponytail: structure + latency = "alive" signal. Phase 2 (server-digest HMAC verify)
            // deferred until we've seen real responses from the 15 labelled proxies.
            return MtProtoDeepProbeResult.success(latencyMs);

        } catch (SocketTimeoutException e) {
            return MtProtoDeepProbeResult.failure(MtProtoProbeFailureCode.IO_ERROR,
                    "No ServerHello within timeout (likely DPI throttle/block)");
        } catch (EOFException e) {
            return MtProtoDeepProbeResult.failure(MtProtoProbeFailureCode.INVALID_RESPONSE,
                    "Connection closed before ServerHello");
        } catch (Exception e) {
            return MtProtoDeepProbeResult.failure(MtProtoProbeFailureCode.CONNECT_ERROR, e.getMessage());
        }
    }

    private byte[] buildFakeTlsClientHello(byte[] key, String domain) throws Exception {
        ByteArrayOutputStream hs = new ByteArrayOutputStream();
        hs.write(new byte[]{0x03, 0x03});            // legacy client version
        hs.write(new byte[32]);                       // random placeholder (digest goes here)
        hs.write(32);                                 // session_id length
        hs.write(randomBytes(32));                    // session_id
        byte[] ciphers = {0x13, 0x01, 0x13, 0x02, 0x13, 0x03, (byte) 0xc0, 0x2f, 0x00, (byte) 0xff};
        hs.write((ciphers.length >> 8) & 0xFF);
        hs.write(ciphers.length & 0xFF);
        hs.write(ciphers);
        hs.write(1);
        hs.write(0);                                  // compression: null
        byte[] ext = buildExtensions(domain);
        hs.write((ext.length >> 8) & 0xFF);
        hs.write(ext.length & 0xFF);
        hs.write(ext);
        byte[] body = hs.toByteArray();

        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(0x01);                              // handshake type: client_hello
        msg.write((body.length >> 16) & 0xFF);
        msg.write((body.length >> 8) & 0xFF);
        msg.write(body.length & 0xFF);
        msg.write(body);
        byte[] handshake = msg.toByteArray();

        ByteArrayOutputStream rec = new ByteArrayOutputStream();
        rec.write(0x16);
        rec.write(0x03);
        rec.write(0x01);                              // record: handshake, legacy version
        rec.write((handshake.length >> 8) & 0xFF);
        rec.write(handshake.length & 0xFF);
        rec.write(handshake);
        byte[] record = rec.toByteArray();

        // ponytail DEBUG-NODE 1 (HMAC scope): digest over the WHOLE record with zeroed random at
        // offset 11. If nothing matches on ground truth, first thing to try is a different scope.
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] digest = mac.doFinal(record);

        // ponytail DEBUG-NODE 2 (timestamp XOR, byte order): last 4 bytes XOR unix time, big-endian.
        // If proxies answer but get flagged dead, try little-endian or drop the XOR.
        // int now = (int) (System.currentTimeMillis() / 1000L);
        // digest[28] ^= (byte) (now >>> 24);
        // digest[29] ^= (byte) (now >>> 16);
        // digest[30] ^= (byte) (now >>> 8);
        // digest[31] ^= (byte) now;

        System.arraycopy(digest, 0, record, 11, 32);  // digest -> random (offset 11)
        return record;
    }

    private byte[] buildExtensions(String domain) throws IOException {
        ByteArrayOutputStream ext = new ByteArrayOutputStream();
        byte[] host = domain.getBytes(StandardCharsets.US_ASCII);
        int nameLen = host.length;
        // SNI (0x0000)
        ext.write(0x00);
        ext.write(0x00);
        int extData = nameLen + 5, listLen = nameLen + 3;
        ext.write((extData >> 8) & 0xFF);
        ext.write(extData & 0xFF);
        ext.write((listLen >> 8) & 0xFF);
        ext.write(listLen & 0xFF);
        ext.write(0x00);                              // host_name
        ext.write((nameLen >> 8) & 0xFF);
        ext.write(nameLen & 0xFF);
        ext.write(host);
        // supported_versions (0x002b) -> TLS 1.3
        ext.write(new byte[]{0x00, 0x2b, 0x00, 0x03, 0x02, 0x03, 0x04});
        // supported_groups (0x000a) -> x25519
        ext.write(new byte[]{0x00, 0x0a, 0x00, 0x04, 0x00, 0x02, 0x00, 0x1d});
        return ext.toByteArray();
    }

    private CryptoSession openObfuscatedSession(Socket socket, byte[] secret) throws Exception {
        byte[] init = generateInitializationPayload();
        byte[] reversed = reverse(init);

        byte[] encryptKey = sha256(concat(slice(init, 8, 40), secret));
        byte[] encryptIv = slice(init, 40, 56);
        byte[] decryptKey = sha256(concat(slice(reversed, 8, 40), secret));
        byte[] decryptIv = slice(reversed, 40, 56);

        Cipher encryptCipher = initCipher(Cipher.ENCRYPT_MODE, encryptKey, encryptIv);
        Cipher decryptCipher = initCipher(Cipher.DECRYPT_MODE, decryptKey, decryptIv);

        byte[] encryptedInit = encryptCipher.update(init);
        byte[] finalInit = concat(slice(init, 0, 56), slice(encryptedInit, 56, 64));

        writeFully(socket.getOutputStream(), finalInit);
        return new CryptoSession(encryptCipher, decryptCipher);
    }

    private byte[] buildProbeRequest(byte[] nonce) {
        byte[] requestBody = concat(intToLittleEndian(REQ_PQ_MULTI_CONSTRUCTOR_ID), nonce);

        ByteBuffer mtProtoPayload = ByteBuffer.allocate(8 + 8 + 4 + requestBody.length).order(ByteOrder.LITTLE_ENDIAN);
        mtProtoPayload.putLong(0L);
        mtProtoPayload.putLong(generateMessageId());
        mtProtoPayload.putInt(requestBody.length);
        mtProtoPayload.put(requestBody);

        byte[] payloadBytes = mtProtoPayload.array();
        byte[] padding = randomBytes(SECURE_RANDOM.nextInt(16));
        byte[] totalLength = intToLittleEndian(payloadBytes.length + padding.length);
        return concat(totalLength, payloadBytes, padding);
    }

    private boolean containsResPq(byte[] decryptedTransportPayload, byte[] expectedNonce) {
        if (decryptedTransportPayload.length < 20) {
            return false;
        }

        long authKeyId = littleEndianLong(decryptedTransportPayload, 0);
        if (authKeyId != 0L) {
            return false;
        }

        int messageDataLength = littleEndianInt(decryptedTransportPayload, 16);
        if (messageDataLength < 20 || 20 + messageDataLength > decryptedTransportPayload.length) {
            return false;
        }

        int constructorId = littleEndianInt(decryptedTransportPayload, 20);
        if (constructorId != RES_PQ_CONSTRUCTOR_ID) {
            return false;
        }

        byte[] nonce = slice(decryptedTransportPayload, 24, 40);
        return MessageDigest.isEqual(nonce, expectedNonce);
    }

    private boolean isTransportError(byte[] payload) {
        return payload.length == 4 && littleEndianInt(payload, 0) < 0;
    }

    private byte[] generateInitializationPayload() {
        while (true) {
            byte[] init = randomBytes(64);
            init[56] = PADDED_INTERMEDIATE_PREFIX;
            init[57] = PADDED_INTERMEDIATE_PREFIX;
            init[58] = PADDED_INTERMEDIATE_PREFIX;
            init[59] = PADDED_INTERMEDIATE_PREFIX;

            short dcId = (short) MT_PROXY_DC_ID;
            init[60] = (byte) (dcId & 0xFF);
            init[61] = (byte) ((dcId >> 8) & 0xFF);

            if (isValidInitializationPayload(init)) {
                return init;
            }
        }
    }

    private boolean isValidInitializationPayload(byte[] init) {
        if ((init[0] & 0xFF) == 0xEF) {
            return false;
        }

        if (startsWith(init, new byte[]{'H', 'E', 'A', 'D'})
                || startsWith(init, new byte[]{'P', 'O', 'S', 'T'})
                || startsWith(init, new byte[]{'G', 'E', 'T', ' '})
                || startsWith(init, new byte[]{'O', 'P', 'T', 'I'})
                || startsWith(init, new byte[]{0x16, 0x03, 0x01, 0x02})
                || startsWith(init, new byte[]{(byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD})
                || startsWith(init, new byte[]{(byte) 0xEE, (byte) 0xEE, (byte) 0xEE, (byte) 0xEE})) {
            return false;
        }

        return !(init[4] == 0 && init[5] == 0 && init[6] == 0 && init[7] == 0);
    }

    private Cipher initCipher(int mode, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher;
    }

    private long generateMessageId() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000;
        long fractional = ((millis % 1000) << 22);
        long messageId = (seconds << 32) | (fractional & 0xFFFFFFFFL);
        return messageId & ~3L;
    }

    private byte[] readFully(InputStream inputStream, int length) throws Exception {
        byte[] buffer = new byte[length];
        int offset = 0;

        while (offset < length) {
            int read = inputStream.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new EOFException("Unexpected end of stream");
            }
            offset += read;
        }

        return buffer;
    }

    private void writeFully(OutputStream outputStream, byte[] payload) throws Exception {
        outputStream.write(payload);
        outputStream.flush();
    }

    private byte[] sha256(byte[] value) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(value);
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private byte[] reverse(byte[] value) {
        byte[] reversed = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            reversed[i] = value[value.length - 1 - i];
        }
        return reversed;
    }

    private byte[] slice(byte[] value, int fromInclusive, int toExclusive) {
        byte[] result = new byte[toExclusive - fromInclusive];
        System.arraycopy(value, fromInclusive, result, 0, result.length);
        return result;
    }

    private byte[] concat(byte[]... values) {
        int totalLength = 0;
        for (byte[] value : values) {
            totalLength += value.length;
        }

        byte[] result = new byte[totalLength];
        int offset = 0;

        for (byte[] value : values) {
            System.arraycopy(value, 0, result, offset, value.length);
            offset += value.length;
        }

        return result;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private int littleEndianInt(byte[] value, int offset) {
        return ByteBuffer.wrap(value, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private long littleEndianLong(byte[] value, int offset) {
        return ByteBuffer.wrap(value, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private byte[] intToLittleEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private record CryptoSession(Cipher encryptCipher, Cipher decryptCipher) {
        private byte[] encrypt(byte[] value) {
            return encryptCipher.update(value);
        }

        private byte[] decrypt(byte[] value) {
            return decryptCipher.update(value);
        }
    }
}
