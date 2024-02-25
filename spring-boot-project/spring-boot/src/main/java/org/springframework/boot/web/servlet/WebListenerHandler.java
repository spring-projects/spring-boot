/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Map;

import jakarta.servlet.annotation.WebListener;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Handler for {@link WebListener @WebListener}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebListenerHandler extends ServletComponentHandler {

	/**
	 * Constructs a new WebListenerHandler object. This constructor calls the constructor
	 * of the superclass, WebListener, passing it the WebListenerHandler class.
	 */
	WebListenerHandler() {
		super(WebListener.class);
	}

	/**
	 * Handles the registration of a ServletComponentWebListenerRegistrar bean definition
	 * in the bean definition registry.
	 * @param attributes the attributes associated with the bean definition
	 * @param beanDefinition the annotated bean definition
	 * @param registry the bean definition registry
	 */
	@Override
	protected void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
			.rootBeanDefinition(ServletComponentWebListenerRegistrar.class);
		builder.addConstructorArgValue(beanDefinition.getBeanClassName());
		registry.registerBeanDefinition(beanDefinition.getBeanClassName() + "Registrar", builder.getBeanDefinition());
	}

	/**
	 * ServletComponentWebListenerRegistrar class.
	 */
	static class ServletComponentWebListenerRegistrar implements WebListenerRegistrar {

		private final String listenerClassName;

		/**
		 * Constructs a new ServletComponentWebListenerRegistrar with the specified
		 * listener class name.
		 * @param listenerClassName the fully qualified class name of the listener to be
		 * registered
		 */
		ServletComponentWebListenerRegistrar(String listenerClassName) {
			this.listenerClassName = listenerClassName;
		}

		/**
		 * Registers a web listener with the given web listener registry.
		 * @param registry the web listener registry to register with
		 */
		@Override
		public void register(WebListenerRegistry registry) {
			registry.addWebListeners(this.listenerClassName);
		}

	}

}
