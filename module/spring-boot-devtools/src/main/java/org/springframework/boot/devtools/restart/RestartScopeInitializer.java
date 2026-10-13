/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Support for a 'restart' {@link Scope} that allows beans to remain between restarts.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class RestartScopeInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.getBeanFactory().registerScope("restart", new RestartScope());
	}

	/**
	 * {@link Scope} that stores beans as {@link Restarter} attributes.
	 */
	private static final class RestartScope implements Scope {

		@Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			return Restarter.getInstance().getOrAddAttribute(name, objectFactory);
		}

		@Override
		public @Nullable Object remove(String name) {
			return Restarter.getInstance().removeAttribute(name);
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback) {
		}

		@Override
		public @Nullable Object resolveContextualObject(String key) {
			return null;
		}

		@Override
		public @Nullable String getConversationId() {
			return null;
		}

	}

}
