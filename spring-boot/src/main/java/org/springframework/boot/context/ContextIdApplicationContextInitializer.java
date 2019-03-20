/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that set the Spring
 * {@link ApplicationContext#getId() ApplicationContext ID}. The following environment
 * properties will be consulted to create the ID:
 * <ul>
 * <li>spring.application.name</li>
 * <li>vcap.application.name</li>
 * <li>spring.config.name</li>
 * </ul>
 * If no property is set the ID 'application' will be used.
 *
 * <p>
 * In addition the following environment properties will be consulted to append a relevant
 * port or index:
 *
 * <ul>
 * <li>spring.application.index</li>
 * <li>vcap.application.instance_index</li>
 * <li>PORT</li>
 * </ul>
 *
 * @author Dave Syer
 */
public class ContextIdApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	/**
	 * Placeholder pattern to resolve for application name. The following order is used to
	 * find the name:
	 * <ul>
	 * <li>{@code spring.application.name}</li>
	 * <li>{@code vcap.application.name}</li>
	 * <li>{@code spring.config.name}</li>
	 * </ul>
	 * This order allows the user defined name to take precedence over the platform
	 * defined name. If no property is defined {@code 'application'} will be used.
	 */
	private static final String NAME_PATTERN = "${spring.application.name:${vcap.application.name:${spring.config.name:application}}}";

	/**
	 * Placeholder pattern to resolve for application index. The following order is used
	 * to find the name:
	 * <ul>
	 * <li>{@code vcap.application.instance_index}</li>
	 * <li>{@code spring.application.index}</li>
	 * <li>{@code server.port}</li>
	 * <li>{@code PORT}</li>
	 * </ul>
	 * This order favors a platform defined index over any user defined value.
	 */
	private static final String INDEX_PATTERN = "${vcap.application.instance_index:${spring.application.index:${server.port:${PORT:null}}}}";

	private final String name;

	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	public ContextIdApplicationContextInitializer() {
		this(NAME_PATTERN);
	}

	/**
	 * Create a new {@link ContextIdApplicationContextInitializer} instance.
	 * @param name the name of the application (can include placeholders)
	 */
	public ContextIdApplicationContextInitializer(String name) {
		this.name = name;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.setId(getApplicationId(applicationContext.getEnvironment()));
	}

	private String getApplicationId(ConfigurableEnvironment environment) {
		String name = environment.resolvePlaceholders(this.name);
		String index = environment.resolvePlaceholders(INDEX_PATTERN);
		String profiles = StringUtils
				.arrayToCommaDelimitedString(environment.getActiveProfiles());
		if (StringUtils.hasText(profiles)) {
			name = name + ":" + profiles;
		}
		if (!"null".equals(index)) {
			name = name + ":" + index;
		}
		return name;
	}

}
