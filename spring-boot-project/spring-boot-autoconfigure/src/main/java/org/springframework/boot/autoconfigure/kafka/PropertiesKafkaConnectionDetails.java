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

/**
 * Adapts {@link KafkaProperties} to {@link KafkaConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesKafkaConnectionDetails implements KafkaConnectionDetails {

	private final int DEFAULT_PORT = 9092;

	private final KafkaProperties properties;

	PropertiesKafkaConnectionDetails(KafkaProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<Node> getBootstrapNodes() {
		return asNodes(this.properties.getBootstrapServers());
	}

	@Override
	public List<Node> getConsumerBootstrapNodes() {
		return bootstrapNodes(this.properties.getConsumer().getBootstrapServers());
	}

	@Override
	public List<Node> getProducerBootstrapNodes() {
		return bootstrapNodes(this.properties.getProducer().getBootstrapServers());
	}

	@Override
	public List<Node> getStreamsBootstrapNodes() {
		return bootstrapNodes(this.properties.getStreams().getBootstrapServers());
	}

	private List<Node> bootstrapNodes(List<String> bootstrapServers) {
		return (bootstrapServers != null) ? asNodes(bootstrapServers) : getBootstrapNodes();
	}

	private List<Node> asNodes(List<String> bootstrapServers) {
		return bootstrapServers.stream().map(this::asNode).toList();
	}

	private Node asNode(String bootstrapNode) {
		int separatorIndex = bootstrapNode.indexOf(':');
		if (separatorIndex == -1) {
			return new Node(bootstrapNode, this.DEFAULT_PORT);
		}
		return new Node(bootstrapNode.substring(0, separatorIndex),
				Integer.parseInt(bootstrapNode.substring(separatorIndex + 1)));
	}

}
