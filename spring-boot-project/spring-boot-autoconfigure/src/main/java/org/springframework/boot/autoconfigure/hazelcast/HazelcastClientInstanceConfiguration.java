/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configuration for Hazelcast client instance.
 *
 * @author Dmytro Nosan
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(HazelcastConnectionDetails.class)
class HazelcastClientInstanceConfiguration {

	@Bean
	HazelcastInstance hazelcastInstance(HazelcastConnectionDetails hazelcastConnectionDetails) {
		ClientConfig config = hazelcastConnectionDetails.getClientConfig();
		return (!StringUtils.hasText(config.getInstanceName())) ? HazelcastClient.newHazelcastClient(config)
				: HazelcastClient.getOrCreateHazelcastClient(config);
	}

}
