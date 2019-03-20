/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import java.lang.reflect.Constructor;

import org.apache.catalina.Context;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link WebSocketContainerCustomizer} for {@link TomcatEmbeddedServletContainerFactory}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public class TomcatWebSocketContainerCustomizer
		extends WebSocketContainerCustomizer<TomcatEmbeddedServletContainerFactory> {

	private static final String TOMCAT_7_LISTENER_TYPE = "org.apache.catalina.deploy.ApplicationListener";

	private static final String TOMCAT_8_LISTENER_TYPE = "org.apache.tomcat.util.descriptor.web.ApplicationListener";

	private static final String WS_LISTENER = "org.apache.tomcat.websocket.server.WsContextListener";

	@Override
	public void doCustomize(TomcatEmbeddedServletContainerFactory tomcatContainer) {
		tomcatContainer.addContextCustomizers(new TomcatContextCustomizer() {
			@Override
			public void customize(Context context) {
				addListener(context, findListenerType());
			}
		});
	}

	private Class<?> findListenerType() {
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
	private void addListener(Context context, Class<?> listenerType) {
		Class<? extends Context> contextClass = context.getClass();
		if (listenerType == null) {
			ReflectionUtils.invokeMethod(ClassUtils.getMethod(contextClass,
					"addApplicationListener", String.class), context, WS_LISTENER);

		}
		else {
			Constructor<?> constructor = ClassUtils
					.getConstructorIfAvailable(listenerType, String.class, boolean.class);
			Object instance = BeanUtils.instantiateClass(constructor, WS_LISTENER, false);
			ReflectionUtils.invokeMethod(ClassUtils.getMethod(contextClass,
					"addApplicationListener", listenerType), context, instance);
		}
	}

}
