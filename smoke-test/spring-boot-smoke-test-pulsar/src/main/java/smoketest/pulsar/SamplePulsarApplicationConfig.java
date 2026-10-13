/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.pulsar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTopic;
import org.springframework.pulsar.core.PulsarTopicBuilder;

@Configuration(proxyBeanMethods = false)
class SamplePulsarApplicationConfig {

	private static final Log logger = LogFactory.getLog(SamplePulsarApplicationConfig.class);

	private static final String TOPIC = "pulsar-smoke-test-topic";

	@Bean
	PulsarTopic pulsarTestTopic() {
		return new PulsarTopicBuilder().name(TOPIC).numberOfPartitions(1).build();
	}

	@Bean
	ApplicationRunner sendMessagesToPulsarTopic(PulsarTemplate<SampleMessage> template) {
		return (args) -> {
			for (int i = 0; i < 10; i++) {
				template.send(TOPIC, new SampleMessage(i, "message:" + i));
				logger.info("++++++PRODUCE:(" + i + ")------");
			}
		};
	}

	@PulsarListener(topics = TOPIC)
	void consumeMessagesFromPulsarTopic(SampleMessage msg) {
		logger.info("++++++CONSUME:(" + msg.id() + ")------");
	}

}
