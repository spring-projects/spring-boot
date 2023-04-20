/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.features.developingautoconfiguration.customstarter.configurationkeys;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("acme")
public class AcmeProperties {

	/**
	 * Whether to check the location of acme resources.
	 */
	private boolean checkLocation = true;

	/**
	 * Timeout for establishing a connection to the acme server.
	 */
	private Duration loginTimeout = Duration.ofSeconds(3);

	// @fold:on // getters/setters ...
	public boolean isCheckLocation() {
		return this.checkLocation;
	}

	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	public Duration getLoginTimeout() {
		return this.loginTimeout;
	}

	public void setLoginTimeout(Duration loginTimeout) {
		this.loginTimeout = loginTimeout;
	}
	// @fold:off

}
