/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.webservices;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for Spring Web Services.
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

	/**
	 * Returns the path of the web service.
	 * @return the path of the web service
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Sets the path for the web service.
	 * @param path the path to be set
	 * @throws IllegalArgumentException if the path is null or has a length less than or
	 * equal to 1
	 * @throws IllegalArgumentException if the path does not start with '/'
	 */
	public void setPath(String path) {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(path.length() > 1, "Path must have length greater than 1");
		Assert.isTrue(path.startsWith("/"), "Path must start with '/'");
		this.path = path;
	}

	/**
	 * Returns the servlet associated with this WebServicesProperties object.
	 * @return the servlet associated with this WebServicesProperties object
	 */
	public Servlet getServlet() {
		return this.servlet;
	}

	/**
	 * Servlet class.
	 */
	public static class Servlet {

		/**
		 * Servlet init parameters to pass to Spring Web Services.
		 */
		private Map<String, String> init = new HashMap<>();

		/**
		 * Load on startup priority of the Spring Web Services servlet.
		 */
		private int loadOnStartup = -1;

		/**
		 * Returns the initial map containing key-value pairs.
		 * @return the initial map containing key-value pairs
		 */
		public Map<String, String> getInit() {
			return this.init;
		}

		/**
		 * Sets the initial parameters for the servlet.
		 * @param init a Map containing the initial parameters as key-value pairs
		 */
		public void setInit(Map<String, String> init) {
			this.init = init;
		}

		/**
		 * Returns the value of the loadOnStartup property.
		 * @return the value of the loadOnStartup property
		 */
		public int getLoadOnStartup() {
			return this.loadOnStartup;
		}

		/**
		 * Sets the value for the load on startup parameter.
		 * @param loadOnStartup the value to set for the load on startup parameter
		 */
		public void setLoadOnStartup(int loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
		}

	}

}
