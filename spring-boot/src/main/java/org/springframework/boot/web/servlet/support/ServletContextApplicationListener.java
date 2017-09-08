/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.servlet.support;

import javax.servlet.ServletContext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.web.context.ConfigurableWebEnvironment;

/**
 * An {@link ApplicationListener} that initializes the {@link SpringApplication} using the
 * {@link ServletContext}.
 *
 * @author Andy Wilkinson
 */
final class ServletContextApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private final ServletContext servletContext;

	ServletContextApplicationListener(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public int getOrder() {
		return ConfigFileApplicationListener.DEFAULT_ORDER - 1;
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		if (event.getEnvironment() instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) event.getEnvironment())
					.initPropertySources(this.servletContext, null);
		}
	}

}
