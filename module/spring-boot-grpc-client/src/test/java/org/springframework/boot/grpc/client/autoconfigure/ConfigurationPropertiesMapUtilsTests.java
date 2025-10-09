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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConfigurationPropertiesMapUtils}.
 */
class ConfigurationPropertiesMapUtilsTests {

	@Test
	void simpleConfigMap() {
		Map<String, Object> map = ConfigurationPropertiesMapUtils
			.convertIntegerKeyedMapsToLists(Map.of("userAgent", "foo", "maxInboundMessageSize", "1MB"));
		assertThat(map).containsEntry("userAgent", "foo").containsEntry("maxInboundMessageSize", "1MB");
	}

	@Test
	void listConfigMap() {
		Map<String, Object> map = ConfigurationPropertiesMapUtils
			.convertIntegerKeyedMapsToLists(Map.of("spam", Map.of("0", "foo", "1", "bar")));
		assertThat(map.get("spam")).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactly("foo", "bar");
	}

	@Test
	void notReallyAListConfigMap() {
		Map<String, Object> map = ConfigurationPropertiesMapUtils
			.convertIntegerKeyedMapsToLists(Map.of("spam", Map.of("0", "foo", "oops", "bar")));
		assertThat(map.get("spam")).asInstanceOf(InstanceOfAssertFactories.MAP).containsKeys("0", "oops");
	}

	@Test
	void methodConfigMap() {
		Map<String, Object> map = ConfigurationPropertiesMapUtils.convertIntegerKeyedMapsToLists(Map.of("methodConfig",
				Map.of("0",
						Map.of("name", Map.of("0", Map.of("service", "foo.Bar", "method", "Baz")), "waitForReady", true,
								"timeout", "1s", "retryPolicy",
								Map.of("maxAttempts", 5, "initialBackoff", "0.1s", "maxBackoff", "1s",
										"backoffMultiplier", 2, "retryableStatusCodes",
										Map.of("0", "UNAVAILABLE", "1", "RESOURCE_EXHAUSTED"))))));

		assertThat(map.get("methodConfig")).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> methodConfigs = (List<Map<String, Object>>) map.get("methodConfig");
		assertThat(methodConfigs).hasSize(1);
		Map<String, Object> methodConfig = methodConfigs.get(0);
		assertThat(methodConfig.get("name")).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> names = (List<Map<String, Object>>) methodConfig.get("name");
		assertThat(names).hasSize(1);
		assertThat(names.get(0)).containsEntry("service", "foo.Bar").containsEntry("method", "Baz");
		assertThat(methodConfig).containsEntry("waitForReady", true);
		assertThat(methodConfig.get("retryPolicy")).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("maxAttempts", 5)
			.containsEntry("maxBackoff", "1s")
			.containsEntry("backoffMultiplier", 2)
			.extractingByKey("retryableStatusCodes")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.contains("UNAVAILABLE", "RESOURCE_EXHAUSTED");
	}

}
