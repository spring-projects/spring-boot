/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.hazelcast;

import java.lang.reflect.Method;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link HealthIndicator} for Hazelcast.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class HazelcastHealthIndicator extends AbstractHealthIndicator {

	private final HazelcastInstance hazelcast;

	public HazelcastHealthIndicator(HazelcastInstance hazelcast) {
		super("Hazelcast health check failed");
		Assert.notNull(hazelcast, "HazelcastInstance must not be null");
		this.hazelcast = hazelcast;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		this.hazelcast.executeTransaction((context) -> {
			builder.up().withDetail("name", this.hazelcast.getName()).withDetail("uuid", extractUuid());
			return null;
		});
	}

	private String extractUuid() {
		try {
			return this.hazelcast.getLocalEndpoint().getUuid().toString();
		}
		catch (NoSuchMethodError ex) {
			// Hazelcast 3
			Method endpointAccessor = ReflectionUtils.findMethod(HazelcastInstance.class, "getLocalEndpoint");
			Object endpoint = ReflectionUtils.invokeMethod(endpointAccessor, this.hazelcast);
			Method uuidAccessor = ReflectionUtils.findMethod(endpoint.getClass(), "getUuid");
			return (String) ReflectionUtils.invokeMethod(uuidAccessor, endpoint);
		}
	}

}
