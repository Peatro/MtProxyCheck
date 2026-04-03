package com.peatroxd.mtprototest.parser.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peatroxd.mtprototest.parser.config.ParserSourcesProperties.SourceDefinition;
import com.peatroxd.mtprototest.parser.config.ParserSourcesProperties.SourceFormat;
import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class HttpProxySource implements ProxySource {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SourceDefinition sourceDefinition;

    public HttpProxySource(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, SourceDefinition sourceDefinition) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.sourceDefinition = sourceDefinition;
    }

    @Override
    public List<RawProxy> fetch() {
        String body = fetchBody(sourceDefinition.getUrl());
        if (body == null || body.isBlank()) {
            log.warn("Received empty body from source {}", sourceName());
            return List.of();
        }

        return switch (sourceDefinition.getFormat()) {
            case TELEGRAM_TEXT -> parseTelegramText(body);
            case JSON_CONNECT_STRING -> parseJsonConnectString(body);
            case URL_POINTER_TELEGRAM_TEXT -> parseUrlPointerTelegramText(body);
        };
    }

    @Override
    public String sourceName() {
        return sourceDefinition.getName();
    }

    private String fetchBody(String url) {
        log.info("Fetching proxies from source='{}' url={}", sourceName(), url);
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }

    private List<RawProxy> parseTelegramText(String body) {
        return body.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(this::parseProxyLink)
                .flatMap(Optional::stream)
                .toList();
    }

    private List<RawProxy> parseJsonConnectString(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.isArray()) {
                log.warn("Source '{}' returned non-array JSON payload", sourceName());
                return List.of();
            }

            return StreamSupport.stream(root.spliterator(), false)
                    .map(node -> node.path("connect_string").asText(null))
                    .filter(value -> value != null && !value.isBlank())
                    .map(this::parseProxyLink)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON source '" + sourceName() + "'", e);
        }
    }

    private List<RawProxy> parseUrlPointerTelegramText(String body) {
        String pointerUrl = body.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Source '" + sourceName() + "' did not provide a redirect URL"));

        String pointedBody = fetchBody(pointerUrl);
        if (pointedBody == null || pointedBody.isBlank()) {
            log.warn("Pointer source '{}' resolved to empty body", sourceName());
            return List.of();
        }

        return parseTelegramText(pointedBody);
    }

    private Optional<RawProxy> parseProxyLink(String link) {
        try {
            URI uri = normalizeTelegramUri(link);
            Map<String, String> params = parseQueryParams(uri.getQuery());

            String server = params.get("server");
            String portRaw = params.get("port");
            String secret = params.get("secret");
            if (server == null || portRaw == null || secret == null) {
                return Optional.empty();
            }

            return Optional.of(RawProxy.builder()
                    .host(server)
                    .port(Integer.parseInt(portRaw))
                    .secret(secret)
                    .type(ProxyType.MTPROTO)
                    .source(sourceName())
                    .build());
        } catch (Exception e) {
            log.debug("Failed to parse proxy link from source='{}': {}", sourceName(), link, e);
            return Optional.empty();
        }
    }

    private URI normalizeTelegramUri(String link) {
        String normalized = link.trim();

        if (normalized.startsWith("tg://")) {
            normalized = normalized.replaceFirst("tg://proxy", "https://dummy.local/proxy");
        } else if (normalized.startsWith("https://t.me/proxy")) {
            return URI.create(normalized);
        } else if (normalized.startsWith("http://t.me/proxy")) {
            return URI.create(normalized);
        } else {
            throw new IllegalArgumentException("Unsupported proxy link format: " + normalized);
        }

        return URI.create(normalized);
    }

    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        return Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> decode(parts[0]),
                        parts -> decode(parts[1]),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
