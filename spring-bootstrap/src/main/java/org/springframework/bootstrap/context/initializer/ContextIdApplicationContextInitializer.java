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

package org.springframework.bootstrap.context.initializer;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Dave Syer
 */
public class ContextIdApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private String name = "${spring.application.name:${vcap.application.name:${spring.config.name:application}}}";

	private int index = -1;

	private int order = Integer.MAX_VALUE - 10;

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
		int index = environment.getProperty("PORT", Integer.class, this.index);
		index = environment.getProperty("vcap.application.instance_index", Integer.class,
				index);
		index = environment.getProperty("spring.application.index", Integer.class, index);
		if (index >= 0) {
			name = name + ":" + index;
		} else {
			String profiles = StringUtils.arrayToCommaDelimitedString(environment
					.getActiveProfiles());
			if (StringUtils.hasText(profiles)) {
				name = name + ":" + profiles;
			}
		}
		return name;
	}

}
