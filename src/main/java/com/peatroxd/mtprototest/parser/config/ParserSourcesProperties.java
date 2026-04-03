package com.peatroxd.mtprototest.parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.sources")
public class ParserSourcesProperties {

    private List<SourceDefinition> entries = new ArrayList<>();

    public List<SourceDefinition> getEntries() {
        return entries;
    }

    public void setEntries(List<SourceDefinition> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    public static class SourceDefinition {
        private String name;
        private String url;
        private SourceFormat format = SourceFormat.TELEGRAM_TEXT;
        private boolean enabled = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public SourceFormat getFormat() {
            return format;
        }

        public void setFormat(SourceFormat format) {
            this.format = format;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public enum SourceFormat {
        TELEGRAM_TEXT,
        JSON_CONNECT_STRING,
        URL_POINTER_TELEGRAM_TEXT
    }
}
