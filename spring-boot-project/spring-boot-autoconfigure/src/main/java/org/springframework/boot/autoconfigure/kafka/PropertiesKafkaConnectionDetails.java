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

	private final KafkaProperties properties;

	PropertiesKafkaConnectionDetails(KafkaProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<String> getBootstrapServers() {
		return this.properties.getBootstrapServers();
	}

	@Override
	public List<String> getConsumerBootstrapServers() {
		return getServers(this.properties.getConsumer().getBootstrapServers());
	}

	@Override
	public List<String> getProducerBootstrapServers() {
		return getServers(this.properties.getProducer().getBootstrapServers());
	}

	@Override
	public List<String> getStreamsBootstrapServers() {
		return getServers(this.properties.getStreams().getBootstrapServers());
	}

	private List<String> getServers(List<String> servers) {
		return (servers != null) ? servers : getBootstrapServers();
	}

}
