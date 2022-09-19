/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.util.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Returns properties from Spring.
 */
public class SpringPropertySource implements PropertySource {

	private static final int DEFAULT_PRIORITY = -100;

	private final Environment environment;

	public SpringPropertySource(Environment environment) {
		this.environment = environment;
	}

	/**
	 * System properties take precendence followed by properties in Log4j properties
	 * files. Spring properties follow.
	 * @return This PropertySource's priority.
	 */
	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

	@Override
	public String getProperty(String key) {
		if (environment != null) {
			return environment.getProperty(key);
		}
		return null;
	}

	@Override
	public boolean containsProperty(String key) {
		if (environment != null) {
			return environment.containsProperty(key);
		}
		return false;
	}

}
