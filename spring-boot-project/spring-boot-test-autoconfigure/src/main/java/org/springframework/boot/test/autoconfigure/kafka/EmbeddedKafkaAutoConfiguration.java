/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.kafka;


import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Kafka.
 *
 * @author Artem Bilan
 * @since 2.2.0
 */
@Configuration
@AutoConfigureBefore(KafkaAutoConfiguration.class)
@ConditionalOnClass(EmbeddedKafka.class)
@ConditionalOnBean(EmbeddedKafkaBroker.class)
public class EmbeddedKafkaAutoConfiguration {

	public EmbeddedKafkaAutoConfiguration(EmbeddedKafkaBroker embeddedKafkaBroker,
			ApplicationContext context) {
		setBrokersProperty(context, embeddedKafkaBroker.getBrokersAsString());
	}

	private void setBrokersProperty(ApplicationContext currentContext, String brokers) {
		if (currentContext instanceof ConfigurableApplicationContext) {
			MutablePropertySources sources = ((ConfigurableApplicationContext) currentContext)
					.getEnvironment().getPropertySources();
			getKafkaBrokers(sources).put("spring.kafka.embedded.brokers", brokers);
		}
		if (currentContext.getParent() != null) {
			setBrokersProperty(currentContext.getParent(), brokers);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getKafkaBrokers(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("kafka.embedded");
		if (propertySource == null) {
			propertySource = new MapPropertySource("kafka.embedded", new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

}
