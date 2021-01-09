/*
 * Copyright 2012-2020 the original author or authors.
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

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Allows for customization of the application's {@link Environment} prior to the
 * application context being refreshed.
 * <p>
 * EnvironmentPostProcessor implementations have to be registered in
 * {@code META-INF/spring.factories}, using the fully qualified name of this class as the
 * key. Implementations may implement the {@link org.springframework.core.Ordered Ordered}
 * interface or use an {@link org.springframework.core.annotation.Order @Order} annotation
 * if they wish to be invoked in specific order.
 * <p>
 * Since Spring Boot 2.4, {@code EnvironmentPostProcessor} implementations may optionally
 * take the following constructor parameters:
 * <ul>
 * <li>{@link DeferredLogFactory} - A factory that can be used to create loggers with
 * output deferred until the application has been full prepared (allowing the environment
 * itself to configure logging levels).</li>
 * <li>{@link Log} - A log with output deferred until the application has been full
 * prepared (allowing the environment itself to configure logging levels).</li>
 * <li>{@link ConfigurableBootstrapContext} - A bootstrap context that can be used to
 * store objects that may be expensive to create, or need to be shared
 * ({@link BootstrapContext} or {@link BootstrapRegistry} may also be used).</li>
 * </ul>
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@FunctionalInterface
public interface EnvironmentPostProcessor {

	/**
	 * Post-process the given {@code environment}.
	 * @param environment the environment to post-process
	 * @param application the application to which the environment belongs
	 */
	void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application);

}
