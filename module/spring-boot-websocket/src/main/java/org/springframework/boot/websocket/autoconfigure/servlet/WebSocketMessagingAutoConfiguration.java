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

package org.springframework.boot.websocket.autoconfigure.servlet;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
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
 * @since 4.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
		"org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration" })
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
public final class WebSocketMessagingAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean({ DelegatingWebSocketMessageBrokerConfiguration.class, JsonMapper.class })
	@ConditionalOnClass({ JsonMapper.class, AbstractMessageBrokerConfiguration.class })
	@Order(0)
	static class SpringBootWebSocketMessageBrokerConfiguration implements WebSocketMessageBrokerConfigurer {

		private final JsonMapper jsonMapper;

		private final @Nullable AsyncTaskExecutor executor;

		SpringBootWebSocketMessageBrokerConfiguration(JsonMapper jsonMapper,
				Map<String, AsyncTaskExecutor> taskExecutors) {
			this.jsonMapper = jsonMapper;
			this.executor = determineAsyncTaskExecutor(taskExecutors);
		}

		private static @Nullable AsyncTaskExecutor determineAsyncTaskExecutor(
				Map<String, AsyncTaskExecutor> taskExecutors) {
			if (taskExecutors.size() == 1) {
				return taskExecutors.values().iterator().next();
			}
			return taskExecutors.get(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
		}

		@Override
		public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
			messageConverters.add(new StringMessageConverter());
			messageConverters.add(new ByteArrayMessageConverter());
			JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(this.jsonMapper);
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			converter.setContentTypeResolver(resolver);
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

	@Order(1)
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.websocket.messaging.preferred-json-mapper", havingValue = "jackson",
			matchIfMissing = true)
	@ConditionalOnClass(JsonMapper.class)
	static class JacksonWebSocketMessageConverterConfiguration implements WebSocketMessageBrokerConfigurer {

		private final JsonMapper jsonMapper;

		JacksonWebSocketMessageConverterConfiguration(JsonMapper jsonMapper) {
			this.jsonMapper = jsonMapper;
		}

		@Override
		public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
			JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(this.jsonMapper);
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			converter.setContentTypeResolver(resolver);
			messageConverters.add(converter);
			return false;
		}

	}

	@Order(1)
	@Configuration(proxyBeanMethods = false)
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	@Conditional(NoJacksonOrJackson2Preferred.class)
	@ConditionalOnClass(ObjectMapper.class)
	static class Jackson2WebSocketMessageConverterConfiguration implements WebSocketMessageBrokerConfigurer {

		private final ObjectMapper objectMapper;

		Jackson2WebSocketMessageConverterConfiguration(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
			org.springframework.messaging.converter.MappingJackson2MessageConverter converter = new org.springframework.messaging.converter.MappingJackson2MessageConverter(
					this.objectMapper);
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			converter.setContentTypeResolver(resolver);
			messageConverters.add(converter);
			return false;
		}

	}

	static class NoJacksonOrJackson2Preferred extends AnyNestedCondition {

		NoJacksonOrJackson2Preferred() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
		static class NoJackson {

		}

		@ConditionalOnProperty(name = "spring.websocket.messaging.preferred-json-mapper", havingValue = "jackson2")
		static class Jackson2Preferred {

		}

	}

}
