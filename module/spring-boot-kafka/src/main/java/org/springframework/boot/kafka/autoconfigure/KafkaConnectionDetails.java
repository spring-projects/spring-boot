/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a Kafka service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface KafkaConnectionDetails extends ConnectionDetails {

	/**
	 * Returns the list of bootstrap servers.
	 * @return the list of bootstrap servers
	 */
	List<String> getBootstrapServers();

	/**
	 * Returns the SSL bundle.
	 * @return the SSL bundle
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

	/**
	 * Returns the security protocol.
	 * @return the security protocol
	 */
	default @Nullable String getSecurityProtocol() {
		return null;
	}

	/**
	 * Returns the consumer configuration.
	 * @return the consumer configuration
	 */
	default Configuration getConsumer() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the producer configuration.
	 * @return the producer configuration
	 */
	default Configuration getProducer() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the admin configuration.
	 * @return the admin configuration
	 */
	default Configuration getAdmin() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the Kafka Streams configuration.
	 * @return the Kafka Streams configuration
	 */
	default Configuration getStreams() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Kafka connection details configuration.
	 */
	interface Configuration {

		/**
		 * Creates a new configuration with the given bootstrap servers.
		 * @param bootstrapServers the bootstrap servers
		 * @return the configuration
		 */
		static Configuration of(List<String> bootstrapServers) {
			return Configuration.of(bootstrapServers, null, null);
		}

		/**
		 * Creates a new configuration with the given bootstrap servers and SSL bundle.
		 * @param bootstrapServers the bootstrap servers
		 * @param sslBundle the SSL bundle
		 * @return the configuration
		 */
		static Configuration of(List<String> bootstrapServers, SslBundle sslBundle) {
			return Configuration.of(bootstrapServers, sslBundle, null);
		}

		/**
		 * Creates a new configuration with the given bootstrap servers, SSL bundle and
		 * security protocol.
		 * @param bootstrapServers the bootstrap servers
		 * @param sslBundle the SSL bundle
		 * @param securityProtocol the security protocol
		 * @return the configuration
		 */
		static Configuration of(List<String> bootstrapServers, @Nullable SslBundle sslBundle,
				@Nullable String securityProtocol) {
			return new Configuration() {
				@Override
				public List<String> getBootstrapServers() {
					return bootstrapServers;
				}

				@Override
				public @Nullable SslBundle getSslBundle() {
					return sslBundle;
				}

				@Override
				public @Nullable String getSecurityProtocol() {
					return securityProtocol;
				}
			};
		}

		/**
		 * Returns the list of bootstrap servers.
		 * @return the list of bootstrap servers
		 */
		List<String> getBootstrapServers();

		/**
		 * Returns the SSL bundle.
		 * @return the SSL bundle
		 */
		default @Nullable SslBundle getSslBundle() {
			return null;
		}

		/**
		 * Returns the security protocol.
		 * @return the security protocol
		 */
		default @Nullable String getSecurityProtocol() {
			return null;
		}

	}

}
