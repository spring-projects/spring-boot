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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
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
	 * Session timeout. If a duration suffix is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration timeout;

	private Servlet servlet = new Servlet();

	/**
     * Returns the timeout duration for the session.
     *
     * @return the timeout duration for the session
     */
    public Duration getTimeout() {
		return this.timeout;
	}

	/**
     * Sets the timeout for the session.
     * 
     * @param timeout the duration of the timeout
     */
    public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	/**
     * Returns the servlet associated with this SessionProperties object.
     *
     * @return the servlet associated with this SessionProperties object
     */
    public Servlet getServlet() {
		return this.servlet;
	}

	/**
     * Sets the servlet for the SessionProperties.
     * 
     * @param servlet the servlet to be set
     */
    public void setServlet(Servlet servlet) {
		this.servlet = servlet;
	}

	/**
	 * Determine the session timeout. If no timeout is configured, the
	 * {@code fallbackTimeout} is used.
	 * @param fallbackTimeout a fallback timeout value if the timeout isn't configured
	 * @return the session timeout
	 * @since 2.4.0
	 */
	public Duration determineTimeout(Supplier<Duration> fallbackTimeout) {
		return (this.timeout != null) ? this.timeout : fallbackTimeout.get();
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
		private Set<DispatcherType> filterDispatcherTypes = new HashSet<>(
				Arrays.asList(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST));

		/**
         * Returns the filter order.
         * 
         * @return the filter order
         */
        public int getFilterOrder() {
			return this.filterOrder;
		}

		/**
         * Sets the filter order.
         * 
         * @param filterOrder the filter order to be set
         */
        public void setFilterOrder(int filterOrder) {
			this.filterOrder = filterOrder;
		}

		/**
         * Returns the set of dispatcher types for which the filter is registered.
         *
         * @return the set of dispatcher types for which the filter is registered
         */
        public Set<DispatcherType> getFilterDispatcherTypes() {
			return this.filterDispatcherTypes;
		}

		/**
         * Sets the dispatcher types for the filter.
         * 
         * @param filterDispatcherTypes the set of dispatcher types for the filter
         */
        public void setFilterDispatcherTypes(Set<DispatcherType> filterDispatcherTypes) {
			this.filterDispatcherTypes = filterDispatcherTypes;
		}

	}

}
