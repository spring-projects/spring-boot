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
package org.springframework.bootstrap.actuate.properties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.bootstrap.context.annotation.ConfigurationProperties;

/**
 * Externalized configuration for endpoints (e.g. paths)
 * 
 * @author Dave Syer
 * 
 */
@ConfigurationProperties(name = "endpoints", ignoreUnknownFields = false)
public class EndpointsProperties {

	@Valid
	private Endpoint info = new Endpoint("/info");

	@Valid
	private Endpoint metrics = new Endpoint("/metrics");

	@Valid
	private Endpoint health = new Endpoint("/health");

	@Valid
	private Endpoint error = new Endpoint("/error");

	@Valid
	private Endpoint shutdown = new Endpoint("/shutdown");

	@Valid
	private Endpoint trace = new Endpoint("/trace");

	@Valid
	private Endpoint dump = new Endpoint("/dump");

	@Valid
	private Endpoint beans = new Endpoint("/beans");

	@Valid
	private Endpoint env = new Endpoint("/env");

	public Endpoint getInfo() {
		return this.info;
	}

	public Endpoint getMetrics() {
		return this.metrics;
	}

	public Endpoint getHealth() {
		return this.health;
	}

	public Endpoint getError() {
		return this.error;
	}

	public Endpoint getShutdown() {
		return this.shutdown;
	}

	public Endpoint getTrace() {
		return this.trace;
	}

	public Endpoint getDump() {
		return this.dump;
	}

	public Endpoint getBeans() {
		return this.beans;
	}

	public Endpoint getEnv() {
		return this.env;
	}

	public static class Endpoint {

		@NotNull
		@Pattern(regexp = "/[^/]*", message = "Path must start with /")
		private String path;

		public Endpoint() {
		}

		public Endpoint(String path) {
			super();
			this.path = path;
		}

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}

	public String[] getSecurePaths() {
		return new String[] { getMetrics().getPath(), getBeans().getPath(),
				getDump().getPath(), getShutdown().getPath(), getTrace().getPath(),
				getEnv().getPath() };
	}

	public String[] getOpenPaths() {
		return new String[] { getHealth().getPath(), getInfo().getPath(),
				getError().getPath() };
	}

}
