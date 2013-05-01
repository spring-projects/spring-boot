/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.service.properties;

import javax.validation.constraints.NotNull;

import org.springframework.bootstrap.context.annotation.ConfigurationProperties;

/**
 * Properties for the management server (e.g. port and path settings).
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "management", ignoreUnknownFields = false)
public class ManagementServerProperties {

	private int port = 8080;

	@NotNull
	private String contextPath = "";

	private boolean allowShutdown = false;

	public boolean isAllowShutdown() {
		return this.allowShutdown;
	}

	public void setAllowShutdown(boolean allowShutdown) {
		this.allowShutdown = allowShutdown;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

}
