/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.rabbit.stream.micrometer.RabbitStreamTemplateObservationConvention;
import org.springframework.rabbit.stream.producer.ProducerCustomizer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

/**
 * Configure {@link RabbitStreamTemplate} with sensible defaults.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RabbitStreamTemplate} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
public class RabbitStreamTemplateConfigurer {

	private @Nullable MessageConverter messageConverter;

	private @Nullable StreamMessageConverter streamMessageConverter;

	private @Nullable ProducerCustomizer producerCustomizer;

	private @Nullable RabbitStreamTemplateObservationConvention observationConvention;

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link StreamMessageConverter} to use or {@code null} if the out-of-the-box
	 * stream message converter should be used.
	 * @param streamMessageConverter the {@link StreamMessageConverter}
	 */
	public void setStreamMessageConverter(@Nullable StreamMessageConverter streamMessageConverter) {
		this.streamMessageConverter = streamMessageConverter;
	}

	/**
	 * Set the {@link ProducerCustomizer} instances to use.
	 * @param producerCustomizer the producer customizer
	 */
	public void setProducerCustomizer(@Nullable ProducerCustomizer producerCustomizer) {
		this.producerCustomizer = producerCustomizer;
	}

	/**
	 * Set the observation convention to use.
	 * @param observationConvention the observation convention to use
	 * @since 4.1.0
	 */
	public void setObservationConvention(@Nullable RabbitStreamTemplateObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

	/**
	 * Configure the specified {@link RabbitStreamTemplate}. The template can be further
	 * tuned and default settings can be overridden.
	 * @param template the {@link RabbitStreamTemplate} instance to configure
	 */
	public void configure(RabbitStreamTemplate template) {
		if (this.messageConverter != null) {
			template.setMessageConverter(this.messageConverter);
		}
		if (this.streamMessageConverter != null) {
			template.setStreamConverter(this.streamMessageConverter);
		}
		if (this.producerCustomizer != null) {
			template.setProducerCustomizer(this.producerCustomizer);
		}
		if (this.observationConvention != null) {
			template.setObservationConvention(this.observationConvention);
		}
	}

}
