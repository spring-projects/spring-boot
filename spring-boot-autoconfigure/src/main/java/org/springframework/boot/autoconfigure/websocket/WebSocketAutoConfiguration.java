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
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.NonEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Auto configuration for websocket server in embedded Tomcat. If
 * <code>spring-websocket</code> is detected on the classpath then we add a listener that
 * installs the Tomcat Websocket initializer. In a non-embedded container it should
 * already be there.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass(name = "org.apache.tomcat.websocket.server.WsSci", value = {
		Servlet.class, Tomcat.class, WebSocketHandler.class })
@AutoConfigureBefore(EmbeddedServletContainerAutoConfiguration.class)
public class WebSocketAutoConfiguration {

	private static final String TOMCAT_7_LISTENER_TYPE = "org.apache.catalina.deploy.ApplicationListener";

	private static final String TOMCAT_8_LISTENER_TYPE = "org.apache.tomcat.util.descriptor.web.ApplicationListener";

	private static final String WS_LISTENER = "org.apache.tomcat.websocket.server.WsContextListener";

	private static Log logger = LogFactory.getLog(WebSocketAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(name = "websocketContainerCustomizer")
	public EmbeddedServletContainerCustomizer websocketContainerCustomizer() {
		return new EmbeddedServletContainerCustomizer() {

			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {
				if (container instanceof NonEmbeddedServletContainerFactory) {
					logger.info("NonEmbeddedServletContainerFactory detected. Websockets "
							+ "support should be native so this normally is not a problem.");
					return;
				}
				Assert.state(container instanceof TomcatEmbeddedServletContainerFactory,
						"Websockets are currently only supported in Tomcat (found "
								+ container.getClass() + "). ");
				TomcatEmbeddedServletContainerFactory tomcatContainer = (TomcatEmbeddedServletContainerFactory) container;
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
	 * Instead of registering the WsSci directly as a ServletContainerInitializer, we use
	 * the ApplicationListener provided by Tomcat. Unfortunately the ApplicationListener
	 * class moved packages in Tomcat 8 and been deleted in 8.0.8 so we have to use
	 * reflection.
	 * @param context the current context
	 * @param listenerType the type of listener to add
	 */
	private static void addListener(Context context, Class<?> listenerType) {
		if (listenerType == null) {
			ReflectionUtils.invokeMethod(ClassUtils.getMethod(context.getClass(),
					"addApplicationListener", String.class), context, WS_LISTENER);

		}
		else {
			Object instance = BeanUtils.instantiateClass(
					ClassUtils.getConstructorIfAvailable(listenerType, String.class,
							boolean.class), WS_LISTENER, false);
			ReflectionUtils.invokeMethod(ClassUtils.getMethod(context.getClass(),
					"addApplicationListener", listenerType), context, instance);
		}
	}
}
