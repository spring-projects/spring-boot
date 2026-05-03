/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.autoconfigure.web.servlet;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.servlet.filter.OrderedFilter;
import org.springframework.boot.web.servlet.DispatcherType;
import org.springframework.core.Ordered;

/**
 * Configuration properties for Spring Security Filter.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@ConfigurationProperties("spring.security.filter")
public class SecurityFilterProperties {

	/**
	 * Order applied to the {@code SecurityFilterChain} that is used to configure basic
	 * authentication for application endpoints. Create your own
	 * {@code SecurityFilterChain} if you want to add your own authentication for all or
	 * some of those endpoints.
	 */
	public static final int BASIC_AUTH_ORDER = Ordered.LOWEST_PRECEDENCE - 5;

	/**
	 * Default order of Spring Security's Filter in the servlet container (i.e. amongst
	 * other filters registered with the container). There is no connection between this
	 * and the {@code @Order} on a {@code SecurityFilterChain}.
	 */
	public static final int DEFAULT_FILTER_ORDER = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100;

	/**
	 * Security filter chain order for Servlet-based web applications.
	 */
	private int order = DEFAULT_FILTER_ORDER;

	/**
	 * Security filter chain dispatcher types for Servlet-based web applications.
	 */
	private Set<DispatcherType> dispatcherTypes = EnumSet.allOf(DispatcherType.class);

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public Set<DispatcherType> getDispatcherTypes() {
		return this.dispatcherTypes;
	}

	public void setDispatcherTypes(Set<DispatcherType> dispatcherTypes) {
		this.dispatcherTypes = dispatcherTypes;
	}

}
