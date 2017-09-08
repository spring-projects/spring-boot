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

package org.springframework.boot.autoconfigure.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.DispatcherType;
import org.springframework.session.web.http.SessionRepositoryFilter;

/**
 * Configuration properties for Spring Session.
 *
 * @author Tommy Ludwig
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.session")
public class SessionProperties {

	/**
	 * Session store type.
	 */
	private StoreType storeType;

	private Integer timeout;

	private Servlet servlet = new Servlet();

	public SessionProperties(ObjectProvider<ServerProperties> serverProperties) {
		ServerProperties properties = serverProperties.getIfUnique();
		this.timeout = (properties != null ? properties.getSession().getTimeout() : null);
	}

	public StoreType getStoreType() {
		return this.storeType;
	}

	public void setStoreType(StoreType storeType) {
		this.storeType = storeType;
	}

	/**
	 * Return the session timeout in seconds.
	 * @return the session timeout in seconds
	 * @see ServerProperties#getSession()
	 */
	public Integer getTimeout() {
		return this.timeout;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

	public void setServlet(Servlet servlet) {
		this.servlet = servlet;
	}

	/**
	 * Servlet-related properties.
	 */
	public static class Servlet {

		/**
		 * Session repository filter order.
		 */
		private int filterOrder = SessionRepositoryFilter.DEFAULT_ORDER;

		/**
		 * Session repository filter dispatcher types.
		 */
		private Set<DispatcherType> filterDispatcherTypes = new HashSet<>(Arrays.asList(
				DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST));

		public int getFilterOrder() {
			return this.filterOrder;
		}

		public void setFilterOrder(int filterOrder) {
			this.filterOrder = filterOrder;
		}

		public Set<DispatcherType> getFilterDispatcherTypes() {
			return this.filterDispatcherTypes;
		}

		public void setFilterDispatcherTypes(Set<DispatcherType> filterDispatcherTypes) {
			this.filterDispatcherTypes = filterDispatcherTypes;
		}

	}

}
