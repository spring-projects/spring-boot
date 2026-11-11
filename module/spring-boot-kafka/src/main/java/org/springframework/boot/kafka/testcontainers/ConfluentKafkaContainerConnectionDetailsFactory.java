/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.testcontainers;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link KafkaConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link ConfluentKafkaContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConfluentKafkaContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ConfluentKafkaContainer, KafkaConnectionDetails> {

	@Override
	protected KafkaConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ConfluentKafkaContainer> source) {
		return new ConfluentKafkaContainerConnectionDetails(source);
	}

	/**
	 * {@link KafkaConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class ConfluentKafkaContainerConnectionDetails
			extends ContainerConnectionDetails<ConfluentKafkaContainer> implements KafkaConnectionDetails {

		private ConfluentKafkaContainerConnectionDetails(ContainerConnectionSource<ConfluentKafkaContainer> source) {
			super(source);
		}

		@Override
		public List<String> getBootstrapServers() {
			return List.of(getContainer().getBootstrapServers());
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

		@Override
		public String getSecurityProtocol() {
			return (getSslBundle() != null) ? "SSL" : "PLAINTEXT";
		}

	}

}
