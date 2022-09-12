/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link ApplicationContextInitializer} wrapper used to initialize a
 * {@link ConfigurableApplicationContext} using artifacts that were generated
 * ahead-of-time.
 *
 * @param <C> the application context type
 * @author Phillip Webb
 * @since 3.0.0
 */
public abstract sealed class AotApplicationContextInitializer<C extends ConfigurableApplicationContext>
		implements ApplicationContextInitializer<C> {

	private static final Log logger = LogFactory.getLog(AotApplicationContextInitializer.class);

	@Override
	public final void initialize(C applicationContext) {
		logger.debug(LogMessage.format("Initializing ApplicationContext using AOT initializer '%s'", getName()));
		aotInitialize(applicationContext);
	}

	abstract void aotInitialize(C applicationContext);

	abstract String getName();

	static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> forMainApplicationClass(
			Class<?> mainApplicationClass) {
		String initializerClassName = mainApplicationClass.getName() + "__ApplicationContextInitializer";
		return new ReflectionDelegatingAotApplicationContextInitializer<>(initializerClassName);
	}

	/**
	 * Create a new {@link AotApplicationContextInitializer} by delegating to an existing
	 * initializer instance.
	 * @param <C> the application context type
	 * @param initializer the initializer to delegate to
	 * @return a new {@link AotApplicationContextInitializer} instance
	 */
	public static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> of(
			ApplicationContextInitializer<C> initializer) {
		Assert.notNull(initializer, "Initializer must not be null");
		return new InstanceDelegatingAotApplicationContextInitializer<>(initializer.getClass().getName(), initializer);
	}

	/**
	 * Create a new {@link AotApplicationContextInitializer} by delegating to an existing
	 * initializer instance.
	 * @param <C> the application context type
	 * @param initializer the initializer to delegate to
	 * @param name the name of the initializer
	 * @return a new {@link AotApplicationContextInitializer} instance
	 */
	public static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> of(String name,
			ApplicationContextInitializer<C> initializer) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(initializer, "Initializer must not be null");
		return new InstanceDelegatingAotApplicationContextInitializer<>(name, initializer);
	}

	/**
	 * {@link AotApplicationContextInitializer} that delegates to an initializer created
	 * via reflection.
	 *
	 * @param <C> the application context type
	 */
	static final class ReflectionDelegatingAotApplicationContextInitializer<C extends ConfigurableApplicationContext>
			extends AotApplicationContextInitializer<C> {

		private final String initializerClassName;

		ReflectionDelegatingAotApplicationContextInitializer(String initializerClassName) {
			this.initializerClassName = initializerClassName;
		}

		@Override
		void aotInitialize(C applicationContext) {
			ApplicationContextInitializer<C> initializer = createInitializer(applicationContext.getClassLoader());
			initializer.initialize(applicationContext);
		}

		@SuppressWarnings("unchecked")
		private ApplicationContextInitializer<C> createInitializer(ClassLoader classLoader) {
			Class<?> initializerClass = ClassUtils.resolveClassName(this.initializerClassName, classLoader);
			Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
			return (ApplicationContextInitializer<C>) BeanUtils.instantiateClass(initializerClass);
		}

		@Override
		String getName() {
			return this.initializerClassName;
		}

	}

	/**
	 * {@link AotApplicationContextInitializer} that delegates to an existing initializer
	 * instance.
	 *
	 * @param <C> the application context type
	 */
	static final class InstanceDelegatingAotApplicationContextInitializer<C extends ConfigurableApplicationContext>
			extends AotApplicationContextInitializer<C> {

		private final String name;

		private final ApplicationContextInitializer<C> initializer;

		InstanceDelegatingAotApplicationContextInitializer(String name, ApplicationContextInitializer<C> initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		void aotInitialize(C applicationContext) {
			this.initializer.initialize(applicationContext);
		}

		@Override
		String getName() {
			return this.name;
		}

	}

}
