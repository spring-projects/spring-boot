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

package org.springframework.boot.support;

import org.apache.commons.logging.Log;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link EnvironmentPostProcessor} to add the {@link RandomValuePropertySource}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class RandomValuePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The default order of this post-processor.
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 1;

	private final Log logger;

	/**
	 * Create a new {@link RandomValuePropertySourceEnvironmentPostProcessor} instance.
	 * @param logFactory the log factory to use
	 * @since 3.0.0
	 */
	public RandomValuePropertySourceEnvironmentPostProcessor(DeferredLogFactory logFactory) {
		this.logger = logFactory.getLog(RandomValuePropertySourceEnvironmentPostProcessor.class);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		RandomValuePropertySource.addToEnvironment(environment, this.logger);
	}

}
