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

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Default {@link ApplicationContextFactory} implementation that will create an
 * appropriate context for the {@link WebApplicationType}.
 *
 * @author Phillip Webb
 */
class DefaultApplicationContextFactory implements ApplicationContextFactory {

	@Override
	public Class<? extends ConfigurableEnvironment> getEnvironmentType(WebApplicationType webApplicationType) {
		return getFromSpringFactories(webApplicationType, ApplicationContextFactory::getEnvironmentType, null);
	}

	@Override
	public ConfigurableEnvironment createEnvironment(WebApplicationType webApplicationType) {
		return getFromSpringFactories(webApplicationType, ApplicationContextFactory::createEnvironment, null);
	}

	@Override
	public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {
		try {
			return getFromSpringFactories(webApplicationType, ApplicationContextFactory::create,
					AnnotationConfigApplicationContext::new);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable create a default ApplicationContext instance, "
					+ "you may need a custom ApplicationContextFactory", ex);
		}
	}

	private <T> T getFromSpringFactories(WebApplicationType webApplicationType,
			BiFunction<ApplicationContextFactory, WebApplicationType, T> action, Supplier<T> defaultResult) {
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
