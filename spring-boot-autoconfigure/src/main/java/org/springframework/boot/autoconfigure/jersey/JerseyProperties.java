/*
 * Copyright 2013-2104 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * {@link ConfigurationProperties} for Jersey.
 *
 * @author Dave Syer
 * @since 1.2.0
 */
@ConfigurationProperties("spring.jersey")
public class JerseyProperties {

	/**
	 * Jersey integration type. Can be either "servlet" or "filter".
	 */
	private Type type = Type.SERVLET;

	/**
	 * Init parameters to pass to Jersey.
	 */
	private Map<String, String> init = new HashMap<String, String>();

	private Filter filter = new Filter();

	public Filter getFilter() {
		return this.filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Map<String, String> getInit() {
		return this.init;
	}

	public void setInit(Map<String, String> init) {
		this.init = init;
	}

	public enum Type {
		SERVLET, FILTER;
	}

	public static class Filter {

		/**
		 * Jersey filter chain order.
		 */
		private int order;

		public int getOrder() {
			return this.order;
		}

		public void setOrder(int order) {
			this.order = order;
		}

	}

}
