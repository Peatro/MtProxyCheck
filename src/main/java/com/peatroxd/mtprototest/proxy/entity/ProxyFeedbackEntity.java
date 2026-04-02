package com.peatroxd.mtprototest.proxy.entity;

import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proxy_id", nullable = false)
    private ProxyEntity proxy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProxyFeedbackResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProxyFeedbackPlatform platform;

    @Column(name = "client_key", nullable = false, length = 128)
    private String clientKey;

    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (platform == null) {
            platform = ProxyFeedbackPlatform.UNKNOWN;
        }
        if (clientKey == null || clientKey.isBlank()) {
            clientKey = "anonymous";
        }
        if (windowStartedAt == null) {
            windowStartedAt = createdAt;
        }
    }
}
