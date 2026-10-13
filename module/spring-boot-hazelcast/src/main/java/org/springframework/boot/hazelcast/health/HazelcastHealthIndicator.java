/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.health;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for Hazelcast.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @author Tommy Karlsson
 * @since 4.0.0
 */
public class HazelcastHealthIndicator extends AbstractHealthIndicator {

	private final HazelcastInstance hazelcast;

	public HazelcastHealthIndicator(HazelcastInstance hazelcast) {
		super("Hazelcast health check failed");
		Assert.notNull(hazelcast, "'hazelcast' must not be null");
		this.hazelcast = hazelcast;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		if (!this.hazelcast.getLifecycleService().isRunning()) {
			builder.down();
			return;
		}
		this.hazelcast.executeTransaction((context) -> {
			String uuid = this.hazelcast.getLocalEndpoint().getUuid().toString();
			builder.up().withDetail("name", this.hazelcast.getName()).withDetail("uuid", uuid);
			return null;
		});
	}

}
