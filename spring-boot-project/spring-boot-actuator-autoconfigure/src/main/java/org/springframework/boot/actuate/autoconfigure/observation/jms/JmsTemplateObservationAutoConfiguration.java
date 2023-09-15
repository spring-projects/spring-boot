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

package org.springframework.boot.actuate.autoconfigure.observation.jms;

import io.micrometer.core.instrument.binder.jms.JmsPublishObservationContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.Message;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumenting
 * {@link JmsTemplate} beans for Observability.
 *
 * @author Brian Clozel
 * @since 3.2.0
 */
@AutoConfiguration(after = { JmsAutoConfiguration.class, ObservationAutoConfiguration.class })
@ConditionalOnBean({ ObservationRegistry.class, JmsTemplate.class })
@ConditionalOnClass({ Observation.class, Message.class, JmsTemplate.class, JmsPublishObservationContext.class })
public class JmsTemplateObservationAutoConfiguration {

	@Bean
	static JmsTemplateObservationPostProcessor jmsTemplateObservationPostProcessor(
			ObjectProvider<ObservationRegistry> observationRegistry) {
		return new JmsTemplateObservationPostProcessor(observationRegistry);
	}

	static class JmsTemplateObservationPostProcessor implements BeanPostProcessor {

		private final ObjectProvider<ObservationRegistry> observationRegistry;

		JmsTemplateObservationPostProcessor(ObjectProvider<ObservationRegistry> observationRegistry) {
			this.observationRegistry = observationRegistry;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof JmsTemplate jmsTemplate) {
				this.observationRegistry.ifAvailable(jmsTemplate::setObservationRegistry);
			}
			return bean;
		}

	}

}
