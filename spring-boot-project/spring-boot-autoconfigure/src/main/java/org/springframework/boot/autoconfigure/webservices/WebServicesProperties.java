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

package org.springframework.boot.autoconfigure.webservices;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link ConfigurationProperties} for Spring Web Services.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.webservices")
public class WebServicesProperties {

	/**
	 * Path that serves as the base URI for the services.
	 */
	private String path = "/services";

	private final Servlet servlet = new Servlet();

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(path.isEmpty() || path.startsWith("/"),
				"Path must start with / or be empty");
		this.path = path;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

	public static class Servlet {

		/**
		 * Servlet init parameters to pass to Spring Web Services.
		 */
		private Map<String, String> init = new HashMap<>();

		/**
		 * Load on startup priority of the Spring Web Services servlet.
		 */
		private int loadOnStartup = -1;

		public Map<String, String> getInit() {
			return this.init;
		}

		public void setInit(Map<String, String> init) {
			this.init = init;
		}

		public int getLoadOnStartup() {
			return this.loadOnStartup;
		}

		public void setLoadOnStartup(int loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
		}

	}

}
