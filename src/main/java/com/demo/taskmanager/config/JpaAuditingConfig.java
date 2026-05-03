package com.demo.taskmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so that {@code @CreatedDate} and
 * {@code @LastModifiedDate} fields on entities are populated automatically
 * by {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
