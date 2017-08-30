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

package org.springframework.boot.autoconfigure.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

/**
 * Properties for the security aspects of an application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@ConfigurationProperties(prefix = "security")
public class SecurityProperties implements SecurityPrerequisite {

	/**
	 * Order applied to the WebSecurityConfigurerAdapter that is used to configure basic
	 * authentication for application endpoints. If you want to add your own
	 * authentication for all or some of those endpoints the best thing to do is to add
	 * your own WebSecurityConfigurerAdapter with lower order.
	 */
	public static final int BASIC_AUTH_ORDER = Ordered.LOWEST_PRECEDENCE - 5;

	/**
	 * Order applied to the WebSecurityConfigurer that ignores standard static resource
	 * paths.
	 */
	public static final int IGNORED_ORDER = Ordered.HIGHEST_PRECEDENCE;

	/**
	 * Default order of Spring Security's Filter in the servlet container (i.e. amongst
	 * other filters registered with the container). There is no connection between this
	 * and the <code>@Order</code> on a WebSecurityConfigurer.
	 */
	public static final int DEFAULT_FILTER_ORDER = FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER
			- 100;

	private Basic basic = new Basic();

	/**
	 * Security filter chain order.
	 */
	private int filterOrder = DEFAULT_FILTER_ORDER;

	/**
	 * Security filter chain dispatcher types.
	 */
	private Set<DispatcherType> filterDispatcherTypes = new HashSet<>(Arrays
			.asList(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST));

	public Basic getBasic() {
		return this.basic;
	}

	public void setBasic(Basic basic) {
		this.basic = basic;
	}

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

	public static class Basic {

		/**
		 * Enable basic authentication.
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
