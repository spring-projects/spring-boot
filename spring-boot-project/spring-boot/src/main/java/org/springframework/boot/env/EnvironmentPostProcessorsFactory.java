/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.env;

import java.util.List;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Factory interface used by the {@link EnvironmentPostProcessorApplicationListener} to
 * create the {@link EnvironmentPostProcessor} instances.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
@FunctionalInterface
public interface EnvironmentPostProcessorsFactory {

	/**
	 * Create all requested {@link EnvironmentPostProcessor} instances.
	 * @param logFactory a deferred log factory
	 * @param bootstrapContext a bootstrap context
	 * @return the post processor instances
	 */
	List<EnvironmentPostProcessor> getEnvironmentPostProcessors(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext);

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} backed by
	 * {@code spring.factories}.
	 * @param classLoader the source class loader
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 */
	static EnvironmentPostProcessorsFactory fromSpringFactories(ClassLoader classLoader) {
		return new ReflectionEnvironmentPostProcessorsFactory(classLoader,
				SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class, classLoader));
	}

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} that reflectively creates post
	 * processors from the given classes.
	 * @param classes the post processor classes
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 */
	static EnvironmentPostProcessorsFactory of(Class<?>... classes) {
		return new ReflectionEnvironmentPostProcessorsFactory(classes);
	}

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} that reflectively creates post
	 * processors from the given class names.
	 * @param classNames the post processor class names
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 */
	static EnvironmentPostProcessorsFactory of(String... classNames) {
		return of(null, classNames);
	}

	/**
	 * Return a {@link EnvironmentPostProcessorsFactory} that reflectively creates post
	 * processors from the given class names.
	 * @param classLoader the source class loader
	 * @param classNames the post processor class names
	 * @return an {@link EnvironmentPostProcessorsFactory} instance
	 * @since 2.4.8
	 */
	static EnvironmentPostProcessorsFactory of(ClassLoader classLoader, String... classNames) {
		return new ReflectionEnvironmentPostProcessorsFactory(classLoader, classNames);
	}

}
