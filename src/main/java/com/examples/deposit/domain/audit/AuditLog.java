package com.examples.deposit.domain.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

	@Id
	private UUID id;

	@Column(name = "entity_type", nullable = false)
	private String entityType;

	@Column(name = "entity_id", nullable = false)
	private UUID entityId;

	@Column(nullable = false)
	private String action;

	@Column(nullable = false)
	private String details;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static AuditLog create(String entityType, UUID entityId, String action, String details) {
		AuditLog auditLog = new AuditLog();
		auditLog.id = UUID.randomUUID();
		auditLog.entityType = entityType;
		auditLog.entityId = entityId;
		auditLog.action = action;
		auditLog.details = details;
		auditLog.createdAt = LocalDateTime.now();
		return auditLog;
	}

}