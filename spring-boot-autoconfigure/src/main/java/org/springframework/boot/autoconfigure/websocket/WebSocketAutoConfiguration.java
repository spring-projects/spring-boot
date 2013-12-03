/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContainerInitializer;

import org.apache.catalina.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;

/**
 * Auto configuration for websocket server (and sockjs in particular). Users should be
 * able to just define beans of type {@link WebSocketHandler}. If
 * <code>spring-websocket</code> is detected on the classpath then we add a
 * {@link DefaultSockJsService} and an MVC handler mapping to
 * <code>/&lt;beanName&gt;/**</code> for all of the <code>WebSocketHandler</code> beans
 * that have a bean name beginning with "/".
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ WebSocketHandler.class })
@AutoConfigureBefore(EmbeddedServletContainerAutoConfiguration.class)
@ConditionalOnMissingBean(WebSocketConfigurer.class)
@EnableWebSocket
public class WebSocketAutoConfiguration {

	private static Log logger = LogFactory.getLog(WebSocketAutoConfiguration.class);

	// Nested class to avoid having to load WebSocketConfigurer before conditions are
	// evaluated
	@Configuration
	protected static class WebSocketRegistrationConfiguration implements
			BeanPostProcessor, BeanFactoryAware, WebSocketConfigurer {

		private Map<String, WebSocketHandler> prefixes = new HashMap<String, WebSocketHandler>();

		private ListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof WebSocketHandler && beanName.startsWith("/")) {
				this.prefixes.put(beanName, (WebSocketHandler) bean);
			}
			return bean;
		}

		private WebSocketHandler getHandler(String prefix) {
			return this.prefixes.get(prefix);
		}

		private String[] getPrefixes() {
			return this.prefixes.keySet().toArray(new String[this.prefixes.size()]);
		}

		@Override
		public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
			// Force initialization of WebSocketHandler beans
			this.beanFactory.getBeansOfType(WebSocketHandler.class);
			for (String prefix : getPrefixes()) {
				logger.info("Adding SockJS handler: " + prefix);
				registry.addHandler(getHandler(prefix), prefix).withSockJS();
			}
		}

	}

	@Configuration
	@ConditionalOnClass(name = "org.apache.tomcat.websocket.server.WsSci")
	protected static class TomcatWebSocketConfiguration {
		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory() {
				@Override
				protected void postProcessContext(Context context) {
					context.addServletContainerInitializer(
							(ServletContainerInitializer) BeanUtils
									.instantiate(ClassUtils.resolveClassName(
											"org.apache.tomcat.websocket.server.WsSci",
											null)), null);
				}
			};
			return factory;
		}
	}

}
