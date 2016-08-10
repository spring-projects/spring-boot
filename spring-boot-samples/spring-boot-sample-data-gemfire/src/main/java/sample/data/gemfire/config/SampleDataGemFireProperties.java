/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.data.gemfire.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gemfire sample.
 *
 * @author John Blum
 */
@ConfigurationProperties(prefix = "sample.data.gemfire")
public class SampleDataGemFireProperties {

	/**
	 * Caching log level.
	 */
	private String logLevel = "config";

	public String getLogLevel() {
		return this.logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

}
