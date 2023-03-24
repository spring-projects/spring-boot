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
	 * Returns the list of bootstrap nodes.
	 * @return the list of bootstrap nodes
	 */
	List<Node> getBootstrapNodes();

	/**
	 * Returns the list of bootstrap nodes used for consumers.
	 * @return the list of bootstrap nodes used for consumers
	 */
	default List<Node> getConsumerBootstrapNodes() {
		return getBootstrapNodes();
	}

	/**
	 * Returns the list of bootstrap nodes used for producers.
	 * @return the list of bootstrap nodes used for producers
	 */
	default List<Node> getProducerBootstrapNodes() {
		return getBootstrapNodes();
	}

	/**
	 * Returns the list of bootstrap nodes used for the admin.
	 * @return the list of bootstrap nodes used for the admin
	 */
	default List<Node> getAdminBootstrapNodes() {
		return getBootstrapNodes();
	}

	/**
	 * Returns the list of bootstrap nodes used for Kafka Streams.
	 * @return the list of bootstrap nodes used for Kafka Streams
	 */
	default List<Node> getStreamsBootstrapNodes() {
		return getBootstrapNodes();
	}

	/**
	 * A Kafka node.
	 *
	 * @param host the hostname
	 * @param port the port
	 */
	record Node(String host, int port) {

	}

}
