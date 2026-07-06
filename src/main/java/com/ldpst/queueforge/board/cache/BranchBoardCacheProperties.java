package com.ldpst.queueforge.board.cache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queueforge.cache.branch-board")
public record BranchBoardCacheProperties(
        boolean enabled,
        Duration ttl
) {
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);

    public BranchBoardCacheProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = DEFAULT_TTL;
        }
    }
}
