/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import java.util.Map;

import jakarta.servlet.annotation.WebListener;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.web.server.servlet.WebListenerRegistrar;
import org.springframework.boot.web.server.servlet.WebListenerRegistry;

/**
 * Handler for {@link WebListener @WebListener}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebListenerHandler extends ServletComponentHandler {

	WebListenerHandler() {
		super(WebListener.class);
	}

	@Override
	protected void doHandle(Map<String, @Nullable Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
			.rootBeanDefinition(ServletComponentWebListenerRegistrar.class);
		builder.addConstructorArgValue(beanDefinition.getBeanClassName());
		registry.registerBeanDefinition(beanDefinition.getBeanClassName() + "Registrar", builder.getBeanDefinition());
	}

	static class ServletComponentWebListenerRegistrar implements WebListenerRegistrar {

		private final String listenerClassName;

		ServletComponentWebListenerRegistrar(String listenerClassName) {
			this.listenerClassName = listenerClassName;
		}

		@Override
		public void register(WebListenerRegistry registry) {
			registry.addWebListeners(this.listenerClassName);
		}

	}

}
