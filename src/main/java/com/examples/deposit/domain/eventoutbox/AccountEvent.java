package com.examples.deposit.domain.eventoutbox;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEvent {

	@Id
	private UUID id;

	@Column(name = "aggregate_type", nullable = false)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private boolean published;

	public static AccountEvent create(String aggregateType, UUID aggregateId, String eventType, String payload) {
		AccountEvent accountEvent = new AccountEvent();
		accountEvent.id = UUID.randomUUID();
		accountEvent.aggregateType = aggregateType;
		accountEvent.aggregateId = aggregateId;
		accountEvent.eventType = eventType;
		accountEvent.payload = payload;
		accountEvent.createdAt = LocalDateTime.now();
		accountEvent.published = false;
		return accountEvent;
	}

}