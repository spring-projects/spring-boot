/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.influx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * Configuration properties for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @since 2.0.0
 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of the
 * <a href="https://github.com/influxdata/influxdb-client-java">new InfluxDB Java
 * client</a> and its own Spring Boot integration.
 */
@Deprecated(since = "3.2.0", forRemoval = true)
@ConfigurationProperties(prefix = "spring.influx")
public class InfluxDbProperties {

	/**
	 * URL of the InfluxDB instance to which to connect.
	 */
	private String url;

	/**
	 * Login user.
	 */
	private String user;

	/**
	 * Login password.
	 */
	private String password;

	@DeprecatedConfigurationProperty(reason = "the new InfluxDb Java client provides Spring Boot integration",
			since = "3.2.0")
	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@DeprecatedConfigurationProperty(reason = "the new InfluxDb Java client provides Spring Boot integration",
			since = "3.2.0")
	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@DeprecatedConfigurationProperty(reason = "the new InfluxDb Java client provides Spring Boot integration",
			since = "3.2.0")
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
