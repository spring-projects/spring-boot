/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket.servlet;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for WebSocket-based messaging.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class WebSocketMessagingAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean({ DelegatingWebSocketMessageBrokerConfiguration.class, ObjectMapper.class })
	@ConditionalOnClass({ ObjectMapper.class, AbstractMessageBrokerConfiguration.class })
	static class WebSocketMessageConverterConfiguration implements WebSocketMessageBrokerConfigurer {

		private final ObjectMapper objectMapper;

		WebSocketMessageConverterConfiguration(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
			MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
			converter.setObjectMapper(this.objectMapper);
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			converter.setContentTypeResolver(resolver);
			messageConverters.add(new StringMessageConverter());
			messageConverters.add(new ByteArrayMessageConverter());
			messageConverters.add(converter);
			return false;
		}

	}

}
