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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.util.PropertySource;

import org.springframework.core.env.Environment;

/**
 * Returns properties from Spring.
 *
 * @author Ralph Goers
 * @since 3.0.0
 */
public class SpringPropertySource implements PropertySource {

	private static final int DEFAULT_PRIORITY = -100;

	private final Environment environment;

	public SpringPropertySource(Environment environment) {
		this.environment = environment;
	}

	/**
	 * System properties take precedence followed by properties in Log4j properties files.
	 * @return this PropertySource's priority.
	 */
	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

	@Override
	public String getProperty(String key) {
		if (this.environment != null) {
			return this.environment.getProperty(key);
		}
		return null;
	}

	@Override
	public boolean containsProperty(String key) {
		if (this.environment != null) {
			return this.environment.containsProperty(key);
		}
		return false;
	}

}
