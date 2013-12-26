/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.initializer;

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
	 * Placeholder pattern to resolve for application name
	 */
	private static final String NAME_PATTERN = "${vcap.application.name:${spring.application.name:${spring.config.name:application}}}";

	/**
	 * Placeholder pattern to resolve for application index
	 */
	private static final String INDEX_PATTERN = "${vcap.application.instance_index:${spring.application.index:${server.port:${PORT:null}}}}";

	private String name;

	private int order = Integer.MAX_VALUE - 10;

	public ContextIdApplicationContextInitializer() {
		this(NAME_PATTERN);
	}

	/**
	 * @param name
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

		String profiles = StringUtils.arrayToCommaDelimitedString(environment
				.getActiveProfiles());
		if (StringUtils.hasText(profiles)) {
			name = name + ":" + profiles;
		}
		if (!"null".equals(index)) {
			name = name + ":" + index;
		}
		return name;
	}

}
