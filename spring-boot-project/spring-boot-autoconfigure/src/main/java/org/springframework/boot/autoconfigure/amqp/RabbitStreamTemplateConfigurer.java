/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.rabbit.stream.producer.ProducerCustomizer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

/**
 * Configure {@link RabbitStreamTemplate} with sensible defaults.
 *
 * @author Eddú Meléndez
 * @since 2.7.0
 */
public class RabbitStreamTemplateConfigurer {

	private MessageConverter messageConverter;

	private StreamMessageConverter streamMessageConverter;

	private ProducerCustomizer producerCustomizer;

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link StreamMessageConverter} to use or {@code null} if the out-of-the-box
	 * stream message converter should be used.
	 * @param streamMessageConverter the {@link StreamMessageConverter}
	 */
	public void setStreamMessageConverter(StreamMessageConverter streamMessageConverter) {
		this.streamMessageConverter = streamMessageConverter;
	}

	/**
	 * Set the {@link ProducerCustomizer} instances to use.
	 * @param producerCustomizer the producer customizer
	 */
	public void setProducerCustomizer(ProducerCustomizer producerCustomizer) {
		this.producerCustomizer = producerCustomizer;
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
	}

}
