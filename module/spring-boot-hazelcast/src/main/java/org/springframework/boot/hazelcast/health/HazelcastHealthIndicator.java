/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		this.hazelcast.executeTransaction((context) -> {
			String uuid = this.hazelcast.getLocalEndpoint().getUuid().toString();
			builder.up().withDetail("name", this.hazelcast.getName()).withDetail("uuid", uuid);
			return null;
		});
	}

}
