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

package org.springframework.boot.kafka.autoconfigure;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties.SimpleAdmin;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Builder for {@link KafkaAdmin}.
 *
 * @author Stephane Nicoll
 */
class KafkaAdminBuilder {

	private final KafkaConfigBuilder kafkaConfigBuilder;

	private final @Nullable KafkaConnectionDetails connectionDetails;

	KafkaAdminBuilder(KafkaProperties properties, @Nullable KafkaConnectionDetails connectionDetails) {
		this.kafkaConfigBuilder = KafkaConfigBuilder.of(properties);
		this.connectionDetails = connectionDetails;
	}

	KafkaAdmin build(SimpleAdmin adminProperties) {
		Map<String, Object> properties = this.kafkaConfigBuilder.admin(adminProperties)
			.withConnectionDetails(this.connectionDetails)
			.build();
		KafkaAdmin kafkaAdmin = new KafkaAdmin(properties);
		if (adminProperties.getCloseTimeout() != null) {
			kafkaAdmin.setCloseTimeout((int) adminProperties.getCloseTimeout().getSeconds());
		}
		if (adminProperties.getOperationTimeout() != null) {
			kafkaAdmin.setOperationTimeout((int) adminProperties.getOperationTimeout().getSeconds());
		}
		kafkaAdmin.setFatalIfBrokerNotAvailable(adminProperties.isFailFast());
		return kafkaAdmin;
	}

}
