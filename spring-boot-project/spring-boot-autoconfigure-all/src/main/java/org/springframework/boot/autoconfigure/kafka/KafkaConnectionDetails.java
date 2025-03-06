/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.util.List;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a Kafka service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
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
	 * @since 3.5.0
	 */
	default SslBundle getSslBundle() {
		return null;
	}

	/**
	 * Returns the security protocol.
	 * @return the security protocol
	 * @since 3.5.0
	 */
	default String getSecurityProtocol() {
		return null;
	}

	/**
	 * Returns the consumer configuration.
	 * @return the consumer configuration
	 * @since 3.5.0
	 */
	default Configuration getConsumer() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the producer configuration.
	 * @return the producer configuration
	 * @since 3.5.0
	 */
	default Configuration getProducer() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the admin configuration.
	 * @return the admin configuration
	 * @since 3.5.0
	 */
	default Configuration getAdmin() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the Kafka Streams configuration.
	 * @return the Kafka Streams configuration
	 * @since 3.5.0
	 */
	default Configuration getStreams() {
		return Configuration.of(getBootstrapServers(), getSslBundle(), getSecurityProtocol());
	}

	/**
	 * Returns the list of bootstrap servers used for consumers.
	 * @return the list of bootstrap servers used for consumers
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of {@link #getConsumer()}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	default List<String> getConsumerBootstrapServers() {
		return getConsumer().getBootstrapServers();
	}

	/**
	 * Returns the list of bootstrap servers used for producers.
	 * @return the list of bootstrap servers used for producers
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of {@link #getProducer()}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	default List<String> getProducerBootstrapServers() {
		return getProducer().getBootstrapServers();
	}

	/**
	 * Returns the list of bootstrap servers used for the admin.
	 * @return the list of bootstrap servers used for the admin
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of {@link #getAdmin()}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	default List<String> getAdminBootstrapServers() {
		return getAdmin().getBootstrapServers();
	}

	/**
	 * Returns the list of bootstrap servers used for Kafka Streams.
	 * @return the list of bootstrap servers used for Kafka Streams
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of {@link #getStreams()}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	default List<String> getStreamsBootstrapServers() {
		return getStreams().getBootstrapServers();
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
		static Configuration of(List<String> bootstrapServers, SslBundle sslBundle, String securityProtocol) {
			return new Configuration() {
				@Override
				public List<String> getBootstrapServers() {
					return bootstrapServers;
				}

				@Override
				public SslBundle getSslBundle() {
					return sslBundle;
				}

				@Override
				public String getSecurityProtocol() {
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
		default SslBundle getSslBundle() {
			return null;
		}

		/**
		 * Returns the security protocol.
		 * @return the security protocol
		 */
		default String getSecurityProtocol() {
			return null;
		}

	}

}
