package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.MtProtoDeepProbeResult;
import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.checker.model.ProxySecretDetails;
import com.peatroxd.mtprototest.checker.model.ProxySecretType;
import com.peatroxd.mtprototest.checker.service.MtProtoDeepProbeService;
import com.peatroxd.mtprototest.checker.service.ProxySecretParser;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class MtProtoDeepProbeServiceImpl implements MtProtoDeepProbeService {

    private static final int CONNECT_TIMEOUT_MS = 2_500;
    private static final int READ_TIMEOUT_MS = 4_000;
    private static final int MAX_PACKET_LENGTH = 4_096;
    private static final int MT_PROXY_DC_ID = 2;
    private static final byte PADDED_INTERMEDIATE_PREFIX = (byte) 0xDD;
    private static final int RES_PQ_CONSTRUCTOR_ID = 0x05162463;
    private static final int REQ_PQ_MULTI_CONSTRUCTOR_ID = 0xBE7E8EF1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ProxySecretParser proxySecretParser;

    public MtProtoDeepProbeServiceImpl(ProxySecretParser proxySecretParser) {
        this.proxySecretParser = proxySecretParser;
    }

    @Override
    public MtProtoDeepProbeResult probe(ProxyEntity proxy) {
        ProxySecretDetails secretDetails = proxySecretParser.parse(proxy.getSecret());
        if (!secretDetails.supported()) {
            MtProtoProbeFailureCode failureCode = secretDetails.type() == ProxySecretType.FAKE_TLS
                    ? MtProtoProbeFailureCode.UNSUPPORTED_SECRET_FORMAT
                    : MtProtoProbeFailureCode.INVALID_SECRET;
            return MtProtoDeepProbeResult.failure(failureCode, secretDetails.message());
        }

        Instant startedAt = Instant.now();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

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
