/*
 * Copyright 2012-2023 the original author or authors.
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
	 * Returns the list of bootstrap servers used for consumers.
	 * @return the list of bootstrap servers used for consumers
	 */
	default List<String> getConsumerBootstrapServers() {
		return getBootstrapServers();
	}

	/**
	 * Returns the list of bootstrap servers used for producers.
	 * @return the list of bootstrap servers used for producers
	 */
	default List<String> getProducerBootstrapServers() {
		return getBootstrapServers();
	}

	/**
	 * Returns the list of bootstrap servers used for the admin.
	 * @return the list of bootstrap servers used for the admin
	 */
	default List<String> getAdminBootstrapServers() {
		return getBootstrapServers();
	}

	/**
	 * Returns the list of bootstrap servers used for Kafka Streams.
	 * @return the list of bootstrap servers used for Kafka Streams
	 */
	default List<String> getStreamsBootstrapServers() {
		return getBootstrapServers();
	}

}
