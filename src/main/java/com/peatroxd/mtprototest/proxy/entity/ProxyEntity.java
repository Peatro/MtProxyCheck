package com.peatroxd.mtprototest.proxy.entity;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "proxies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_proxy_host_port_secret_type",
                        columnNames = {"host", "port", "secret", "type"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String secret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProxyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProxyType type;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private ProxyVerificationStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ProxyModerationStatus moderationStatus;

    @Column(name = "last_latency_ms")
    private Long lastLatencyMs;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures;

    @Column(name = "consecutive_successes", nullable = false)
    private Integer consecutiveSuccesses;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;

        if (status == null) {
            status = ProxyStatus.NEW;
        }
        if (score == null) {
            score = 0;
        }
        if (verificationStatus == null) {
            verificationStatus = ProxyVerificationStatus.UNVERIFIED;
        }
        if (moderationStatus == null) {
            moderationStatus = ProxyModerationStatus.NORMAL;
        }
        if (consecutiveFailures == null) {
            consecutiveFailures = 0;
        }
        if (consecutiveSuccesses == null) {
            consecutiveSuccesses = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
