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

package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * External configuration properties for a {@link JtaTransactionManager} created by
 * Spring. All {@literal spring.jta.} properties are also applied to the appropriate
 * vendor specific configuration.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = JtaProperties.PREFIX, ignoreUnknownFields = true)
public class JtaProperties {

	public static final String PREFIX = "spring.jta";

	private String logDir;

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	public String getLogDir() {
		return this.logDir;
	}

}
