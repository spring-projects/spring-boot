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

package org.springframework.boot.rsocket.autoconfigure;

import io.rsocket.transport.netty.server.TcpServerTransport;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.web.method.ControllerAdviceBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring RSocket support in Spring
 * Messaging.
 *
 * @author Brian Clozel
 * @author Dmitry Sulman
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = RSocketStrategiesAutoConfiguration.class)
@ConditionalOnClass({ RSocketRequester.class, io.rsocket.RSocket.class, TcpServerTransport.class })
public final class RSocketMessagingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	RSocketMessageHandler messageHandler(RSocketStrategies rSocketStrategies,
			ObjectProvider<RSocketMessageHandlerCustomizer> customizers, ApplicationContext context) {
		RSocketMessageHandler messageHandler = new RSocketMessageHandler();
		messageHandler.setRSocketStrategies(rSocketStrategies);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(messageHandler));

		return messageHandler;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ControllerAdviceBean.class)
	static class MessagingAdviceConfiguration {

		@Bean
		MessagingAdviceRSocketMessageHandlerCustomizer messagingAdviceRSocketMessageHandlerCustomizer(
				ApplicationContext applicationContext) {
			return new MessagingAdviceRSocketMessageHandlerCustomizer(applicationContext);
		}

	}

	static final class MessagingAdviceRSocketMessageHandlerCustomizer implements RSocketMessageHandlerCustomizer {

		private final ApplicationContext applicationContext;

		MessagingAdviceRSocketMessageHandlerCustomizer(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void customize(RSocketMessageHandler messageHandler) {
			ControllerAdviceBean.findAnnotatedBeans(this.applicationContext)
				.forEach((controllerAdviceBean) -> messageHandler
					.registerMessagingAdvice(new ControllerAdviceBeanWrapper(controllerAdviceBean)));
		}

	}

	private static final class ControllerAdviceBeanWrapper implements MessagingAdviceBean {

		private final ControllerAdviceBean adviceBean;

		private ControllerAdviceBeanWrapper(ControllerAdviceBean adviceBean) {
			this.adviceBean = adviceBean;
		}

		@Override
		public @Nullable Class<?> getBeanType() {
			return this.adviceBean.getBeanType();
		}

		@Override
		public Object resolveBean() {
			return this.adviceBean.resolveBean();
		}

		@Override
		public boolean isApplicableToBeanType(Class<?> beanType) {
			return this.adviceBean.isApplicableToBeanType(beanType);
		}

		@Override
		public int getOrder() {
			return this.adviceBean.getOrder();
		}

	}

}
