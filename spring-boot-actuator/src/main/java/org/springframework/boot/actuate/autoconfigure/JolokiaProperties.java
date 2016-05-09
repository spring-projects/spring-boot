/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Jolokia.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "jolokia")
public class JolokiaProperties {

	/**
	 * Jolokia settings. These are traditionally set using servlet parameters. Refer to
	 * the documentation of Jolokia for more details.
	 */
	private Map<String, String> config = new HashMap<String, String>();

	public Map<String, String> getConfig() {
		return this.config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
	}
}
