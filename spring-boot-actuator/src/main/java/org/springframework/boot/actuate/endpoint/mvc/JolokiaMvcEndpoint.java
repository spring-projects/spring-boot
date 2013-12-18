/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link Endpoint} implementation to register the Jolokia infrastructure with the Boot
 * management subsystem.
 * 
 * @author Christian Dupuis
 */
@ConfigurationProperties(name = "endpoints.jolokia", ignoreUnknownFields = false)
public class JolokiaMvcEndpoint implements MvcEndpoint {

	@NotNull
	@Pattern(regexp = "/[^/]*", message = "Path must start with /")
	private String path;

	private boolean sensitive;

	private boolean enabled = true;

	public JolokiaMvcEndpoint() {
		this.path = "/jolokia";
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	public Class<?> getEndpointType() {
		return null;
	}

}
