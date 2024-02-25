/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jersey;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for Jersey.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.jersey")
public class JerseyProperties {

	/**
	 * Jersey integration type.
	 */
	private Type type = Type.SERVLET;

	/**
	 * Init parameters to pass to Jersey through the servlet or filter.
	 */
	private Map<String, String> init = new HashMap<>();

	private final Filter filter = new Filter();

	private final Servlet servlet = new Servlet();

	/**
	 * Path that serves as the base URI for the application. If specified, overrides the
	 * value of "@ApplicationPath".
	 */
	private String applicationPath;

	/**
	 * Returns the filter associated with this JerseyProperties object.
	 * @return the filter associated with this JerseyProperties object
	 */
	public Filter getFilter() {
		return this.filter;
	}

	/**
	 * Returns the servlet associated with this JerseyProperties object.
	 * @return the servlet associated with this JerseyProperties object
	 */
	public Servlet getServlet() {
		return this.servlet;
	}

	/**
	 * Returns the type of the JerseyProperties object.
	 * @return the type of the JerseyProperties object
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Sets the type of the JerseyProperties.
	 * @param type the type to be set
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Returns the initial map of key-value pairs.
	 * @return the initial map of key-value pairs
	 */
	public Map<String, String> getInit() {
		return this.init;
	}

	/**
	 * Sets the initial values for the JerseyProperties object.
	 * @param init a Map containing the initial values to be set
	 */
	public void setInit(Map<String, String> init) {
		this.init = init;
	}

	/**
	 * Returns the application path.
	 * @return the application path
	 */
	public String getApplicationPath() {
		return this.applicationPath;
	}

	/**
	 * Sets the application path for the JerseyProperties class.
	 * @param applicationPath the application path to be set
	 */
	public void setApplicationPath(String applicationPath) {
		this.applicationPath = applicationPath;
	}

	public enum Type {

		SERVLET, FILTER

	}

	/**
	 * Filter class.
	 */
	public static class Filter {

		/**
		 * Jersey filter chain order.
		 */
		private int order;

		/**
		 * Returns the order of the filter.
		 * @return the order of the filter
		 */
		public int getOrder() {
			return this.order;
		}

		/**
		 * Sets the order of the filter.
		 * @param order the order of the filter
		 */
		public void setOrder(int order) {
			this.order = order;
		}

	}

	/**
	 * Servlet class.
	 */
	public static class Servlet {

		/**
		 * Load on startup priority of the Jersey servlet.
		 */
		private int loadOnStartup = -1;

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
