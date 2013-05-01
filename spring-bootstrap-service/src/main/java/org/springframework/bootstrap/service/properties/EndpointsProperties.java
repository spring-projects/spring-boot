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
 * Externalized configuration for endpoints (e.g. paths)
 * 
 * @author Dave Syer
 * 
 */
@ConfigurationProperties(name = "endpoints", ignoreUnknownFields = false)
public class EndpointsProperties {

	private Endpoint varz = new Endpoint("/varz");

	private Endpoint healthz = new Endpoint("/healthz");

	private Endpoint error = new Endpoint("/error");

	private Endpoint shutdown = new Endpoint("/shutdown");

	private Endpoint trace = new Endpoint("/trace");

	public Endpoint getVarz() {
		return this.varz;
	}

	public Endpoint getHealthz() {
		return this.healthz;
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

	public static class Endpoint {

		@NotNull
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

}
