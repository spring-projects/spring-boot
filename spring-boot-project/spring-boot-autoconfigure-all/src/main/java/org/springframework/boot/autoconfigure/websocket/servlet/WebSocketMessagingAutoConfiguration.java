/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for WebSocket-based messaging.
 *
 * @author Andy Wilkinson
 * @author Lasse Wulff
 * @author Moritz Halbritter
 * @since 1.3.0
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
public class WebSocketMessagingAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean({ DelegatingWebSocketMessageBrokerConfiguration.class, ObjectMapper.class })
	@ConditionalOnClass({ ObjectMapper.class, AbstractMessageBrokerConfiguration.class })
	@Order(0)
	static class WebSocketMessageConverterConfiguration implements WebSocketMessageBrokerConfigurer {

		private final ObjectMapper objectMapper;

		private final AsyncTaskExecutor executor;

		WebSocketMessageConverterConfiguration(ObjectMapper objectMapper,
				Map<String, AsyncTaskExecutor> taskExecutors) {
			this.objectMapper = objectMapper;
			this.executor = determineAsyncTaskExecutor(taskExecutors);
		}

		private static AsyncTaskExecutor determineAsyncTaskExecutor(Map<String, AsyncTaskExecutor> taskExecutors) {
			if (taskExecutors.size() == 1) {
				return taskExecutors.values().iterator().next();
			}
			return taskExecutors.get(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
		}

		@Override
		public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
			MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter(this.objectMapper);
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			converter.setContentTypeResolver(resolver);
			messageConverters.add(new StringMessageConverter());
			messageConverters.add(new ByteArrayMessageConverter());
			messageConverters.add(converter);
			return false;
		}

		@Override
		public void configureClientInboundChannel(ChannelRegistration registration) {
			if (this.executor != null) {
				registration.executor(this.executor);
			}
		}

		@Override
		public void configureClientOutboundChannel(ChannelRegistration registration) {
			if (this.executor != null) {
				registration.executor(this.executor);
			}
		}

		@Bean
		static LazyInitializationExcludeFilter eagerStompWebSocketHandlerMapping() {
			return (name, definition, type) -> name.equals("stompWebSocketHandlerMapping");
		}

	}

}
