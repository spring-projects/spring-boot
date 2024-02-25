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

package smoketest.pulsar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTopic;

/**
 * ImperativeAppConfig class.
 */
@Configuration(proxyBeanMethods = false)
@Profile("smoketest.pulsar.imperative")
class ImperativeAppConfig {

	private static final Log logger = LogFactory.getLog(ImperativeAppConfig.class);

	private static final String TOPIC = "pulsar-smoke-test-topic";

	/**
     * Creates a Pulsar topic with the specified topic name and number of partitions.
     *
     * @return the PulsarTopic object representing the created topic
     */
    @Bean
	PulsarTopic pulsarTestTopic() {
		return PulsarTopic.builder(TOPIC).numberOfPartitions(1).build();
	}

	/**
     * Sends messages to a Pulsar topic using the provided PulsarTemplate.
     * 
     * @param template the PulsarTemplate used to send messages
     * @return an ApplicationRunner that sends messages to the Pulsar topic
     */
    @Bean
	ApplicationRunner sendMessagesToPulsarTopic(PulsarTemplate<SampleMessage> template) {
		return (args) -> {
			for (int i = 0; i < 10; i++) {
				template.send(TOPIC, new SampleMessage(i, "message:" + i));
				logger.info("++++++PRODUCE IMPERATIVE:(" + i + ")------");
			}
		};
	}

	/**
     * This method is used to consume messages from a Pulsar topic.
     * 
     * @param msg The message to be consumed from the Pulsar topic.
     */
    @PulsarListener(topics = TOPIC)
	void consumeMessagesFromPulsarTopic(SampleMessage msg) {
		logger.info("++++++CONSUME IMPERATIVE:(" + msg.id() + ")------");
	}

}
