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

package org.springframework.boot;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Contract;

/**
 * Default {@link ApplicationContextFactory} implementation that will create an
 * appropriate context for the {@link WebApplicationType}.
 *
 * @author Phillip Webb
 */
class DefaultApplicationContextFactory implements ApplicationContextFactory {

	// Method reference is not detected with correct nullability
	@SuppressWarnings("NullAway")
	@Override
	public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(
			@Nullable WebApplicationType webApplicationType) {
		return getFromSpringFactories(webApplicationType, ApplicationContextFactory::getEnvironmentType, null);
	}

	// Method reference is not detected with correct nullability
	@SuppressWarnings("NullAway")
	@Override
	public @Nullable ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
		return getFromSpringFactories(webApplicationType, ApplicationContextFactory::createEnvironment, null);
	}

	// Method reference is not detected with correct nullability
	@SuppressWarnings("NullAway")
	@Override
	public ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType) {
		try {
			return getFromSpringFactories(webApplicationType, ApplicationContextFactory::create,
					this::createDefaultApplicationContext);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable create a default ApplicationContext instance, "
					+ "you may need a custom ApplicationContextFactory", ex);
		}
	}

	private ConfigurableApplicationContext createDefaultApplicationContext() {
		if (!AotDetector.useGeneratedArtifacts()) {
			return new AnnotationConfigApplicationContext();
		}
		return new GenericApplicationContext();
	}

	@Contract("_, _, !null -> !null")
	private <T> @Nullable T getFromSpringFactories(@Nullable WebApplicationType webApplicationType,
			BiFunction<ApplicationContextFactory, @Nullable WebApplicationType, @Nullable T> action,
			@Nullable Supplier<T> defaultResult) {
		for (ApplicationContextFactory candidate : SpringFactoriesLoader.loadFactories(ApplicationContextFactory.class,
				getClass().getClassLoader())) {
			T result = action.apply(candidate, webApplicationType);
			if (result != null) {
				return result;
			}
		}
		return (defaultResult != null) ? defaultResult.get() : null;
	}

}
