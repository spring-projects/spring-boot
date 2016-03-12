/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.env;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * {@link EnvironmentPostProcessor} to add properties that make sense when working at
 * development time.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DevToolsPropertyDefaultsPostProcessor implements EnvironmentPostProcessor {

	private static final Map<String, Object> PROPERTIES;

	static {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.thymeleaf.cache", "false");
		properties.put("spring.freemarker.cache", "false");
		properties.put("spring.groovy.template.cache", "false");
		properties.put("spring.velocity.cache", "false");
		properties.put("spring.mustache.cache", "false");
		properties.put("server.session.persistent", "true");
		properties.put("spring.h2.console.enabled", "true");
		properties.put("spring.resources.cache-period", "0");
		PROPERTIES = Collections.unmodifiableMap(properties);
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		if (isLocalApplication(environment)) {
			PropertySource<?> propertySource = new MapPropertySource("refresh",
					PROPERTIES);
			environment.getPropertySources().addLast(propertySource);
		}
	}

	private boolean isLocalApplication(ConfigurableEnvironment environment) {
		return environment.getPropertySources().get("remoteUrl") == null;
	}

}
