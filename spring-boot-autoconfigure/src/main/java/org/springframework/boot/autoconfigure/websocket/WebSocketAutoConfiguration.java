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

import javax.servlet.Servlet;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.NonEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Auto configuration for websocket server in embedded Tomcat or Jetty. Requires
 * <code>spring-websocket</code> and either Tomcat or Jetty with their WebSocket modules
 * to be on the classpath.
 * <p/>
 * If Tomcat's WebSocket support is detected on the classpath we add a listener that
 * installs the Tomcat Websocket initializer. In a non-embedded container it should
 * already be there.
 * <p/>
 * If Jetty's WebSocket support is detected on the classpath we add a configuration that
 * configures the context with WebSocket support. In a non-embedded container it should
 * already be there.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnClass({ Servlet.class, WebSocketHandler.class })
@AutoConfigureBefore(EmbeddedServletContainerAutoConfiguration.class)
public class WebSocketAutoConfiguration {

	@Configuration
	@ConditionalOnClass(name = "org.apache.tomcat.websocket.server.WsSci", value = Tomcat.class)
	static class TomcatWebSocketConfiguration {

		private static final String TOMCAT_7_LISTENER_TYPE = "org.apache.catalina.deploy.ApplicationListener";

		private static final String TOMCAT_8_LISTENER_TYPE = "org.apache.tomcat.util.descriptor.web.ApplicationListener";

		private static final String WS_LISTENER = "org.apache.tomcat.websocket.server.WsContextListener";

		@Bean
		@ConditionalOnMissingBean(name = "websocketContainerCustomizer")
		public EmbeddedServletContainerCustomizer websocketContainerCustomizer() {
			return new WebSocketContainerCustomizer<TomcatEmbeddedServletContainerFactory>(
					TomcatEmbeddedServletContainerFactory.class) {

				@Override
				public void doCustomize(
						TomcatEmbeddedServletContainerFactory tomcatContainer) {
					tomcatContainer.addContextCustomizers(new TomcatContextCustomizer() {
						@Override
						public void customize(Context context) {
							addListener(context, findListenerType());
						}
					});
				}

			};
		}

		private static Class<?> findListenerType() {
			if (ClassUtils.isPresent(TOMCAT_7_LISTENER_TYPE, null)) {
				return ClassUtils.resolveClassName(TOMCAT_7_LISTENER_TYPE, null);
			}
			if (ClassUtils.isPresent(TOMCAT_8_LISTENER_TYPE, null)) {
				return ClassUtils.resolveClassName(TOMCAT_8_LISTENER_TYPE, null);
			}
			// With Tomcat 8.0.8 ApplicationListener is not required
			return null;
		}

		/**
		 * Instead of registering the WsSci directly as a ServletContainerInitializer, we
		 * use the ApplicationListener provided by Tomcat. Unfortunately the
		 * ApplicationListener class moved packages in Tomcat 8 and been deleted in 8.0.8
		 * so we have to use reflection.
		 * @param context the current context
		 * @param listenerType the type of listener to add
		 */
		private static void addListener(Context context, Class<?> listenerType) {
			if (listenerType == null) {
				ReflectionUtils.invokeMethod(ClassUtils.getMethod(context.getClass(),
						"addApplicationListener", String.class), context, WS_LISTENER);

			}
			else {
				Object instance = BeanUtils.instantiateClass(ClassUtils
						.getConstructorIfAvailable(listenerType, String.class,
								boolean.class), WS_LISTENER, false);
				ReflectionUtils.invokeMethod(ClassUtils.getMethod(context.getClass(),
						"addApplicationListener", listenerType), context, instance);
			}
		}
	}

	@Configuration
	@ConditionalOnClass(WebSocketServerContainerInitializer.class)
	static class JettyWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketContainerCustomizer")
		public EmbeddedServletContainerCustomizer websocketContainerCustomizer() {
			return new WebSocketContainerCustomizer<JettyEmbeddedServletContainerFactory>(
					JettyEmbeddedServletContainerFactory.class) {

				@Override
				protected void doCustomize(JettyEmbeddedServletContainerFactory container) {
					container.addConfigurations(new AbstractConfiguration() {

						@Override
						public void configure(WebAppContext context) throws Exception {
							WebSocketServerContainerInitializer.configureContext(context);
						}

					});
				}

			};
		}

	}

	abstract static class WebSocketContainerCustomizer<T extends ConfigurableEmbeddedServletContainer>
			implements EmbeddedServletContainerCustomizer {

		private Log logger = LogFactory.getLog(getClass());

		private final Class<T> containerType;

		protected WebSocketContainerCustomizer(Class<T> containerType) {
			this.containerType = containerType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void customize(ConfigurableEmbeddedServletContainer container) {
			if (container instanceof NonEmbeddedServletContainerFactory) {
				this.logger
						.info("NonEmbeddedServletContainerFactory detected. Websockets "
								+ "support should be native so this normally is not a problem.");
				return;
			}
			if (this.containerType.isAssignableFrom(container.getClass())) {
				doCustomize((T) container);
			}
		}

		protected abstract void doCustomize(T container);

	}

}
