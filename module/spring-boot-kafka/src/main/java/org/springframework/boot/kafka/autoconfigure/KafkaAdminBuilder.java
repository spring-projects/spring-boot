/*
 * Copyright 2012-present the original author or authors.
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
