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

package org.springframework.boot.actuate.autoconfigure.jolokia;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Jolokia.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.jolokia")
public class JolokiaProperties {

	/**
	 * Enable Jolokia.
	 */
	private boolean enabled;

	/**
	 * Path at which Jolokia will be available.
	 */
	private String path = "/jolokia";

	/**
	 * Jolokia settings. These are traditionally set using servlet parameters. Refer to
	 * the documentation of Jolokia for more details.
	 */
	private final Map<String, String> config = new HashMap<>();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, String> getConfig() {
		return this.config;
	}

}
