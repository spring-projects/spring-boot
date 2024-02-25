/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.DispatcherType;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Spring Security.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.security")
public class SecurityProperties {

	/**
	 * Order applied to the {@code SecurityFilterChain} that is used to configure basic
	 * authentication for application endpoints. Create your own
	 * {@code SecurityFilterChain} if you want to add your own authentication for all or
	 * some of those endpoints.
	 */
	public static final int BASIC_AUTH_ORDER = Ordered.LOWEST_PRECEDENCE - 5;

	/**
	 * Order applied to the {@code WebSecurityCustomizer} that ignores standard static
	 * resource paths.
	 */
	public static final int IGNORED_ORDER = Ordered.HIGHEST_PRECEDENCE;

	/**
	 * Default order of Spring Security's Filter in the servlet container (i.e. amongst
	 * other filters registered with the container). There is no connection between this
	 * and the {@code @Order} on a {@code SecurityFilterChain}.
	 */
	public static final int DEFAULT_FILTER_ORDER = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100;

	private final Filter filter = new Filter();

	private final User user = new User();

	/**
	 * Returns the User object associated with this SecurityProperties instance.
	 * @return the User object associated with this SecurityProperties instance
	 */
	public User getUser() {
		return this.user;
	}

	/**
	 * Returns the filter associated with the SecurityProperties object.
	 * @return the filter associated with the SecurityProperties object
	 */
	public Filter getFilter() {
		return this.filter;
	}

	/**
	 * Filter class.
	 */
	public static class Filter {

		/**
		 * Security filter chain order for Servlet-based web applications.
		 */
		private int order = DEFAULT_FILTER_ORDER;

		/**
		 * Security filter chain dispatcher types for Servlet-based web applications.
		 */
		private Set<DispatcherType> dispatcherTypes = EnumSet.allOf(DispatcherType.class);

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

		/**
		 * Returns the dispatcher types of this filter.
		 * @return the dispatcher types of this filter
		 */
		public Set<DispatcherType> getDispatcherTypes() {
			return this.dispatcherTypes;
		}

		/**
		 * Sets the dispatcher types for this filter.
		 * @param dispatcherTypes the set of dispatcher types to be set
		 */
		public void setDispatcherTypes(Set<DispatcherType> dispatcherTypes) {
			this.dispatcherTypes = dispatcherTypes;
		}

	}

	/**
	 * User class.
	 */
	public static class User {

		/**
		 * Default user name.
		 */
		private String name = "user";

		/**
		 * Password for the default user name.
		 */
		private String password = UUID.randomUUID().toString();

		/**
		 * Granted roles for the default user name.
		 */
		private List<String> roles = new ArrayList<>();

		private boolean passwordGenerated = true;

		/**
		 * Returns the name of the User.
		 * @return the name of the User
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Sets the name of the user.
		 * @param name the name to be set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Returns the password of the User.
		 * @return the password of the User
		 */
		public String getPassword() {
			return this.password;
		}

		/**
		 * Sets the password for the user.
		 * @param password the password to be set
		 */
		public void setPassword(String password) {
			if (!StringUtils.hasLength(password)) {
				return;
			}
			this.passwordGenerated = false;
			this.password = password;
		}

		/**
		 * Returns the list of roles associated with the user.
		 * @return the list of roles
		 */
		public List<String> getRoles() {
			return this.roles;
		}

		/**
		 * Sets the roles for the user.
		 * @param roles the list of roles to be set
		 */
		public void setRoles(List<String> roles) {
			this.roles = new ArrayList<>(roles);
		}

		/**
		 * Returns a boolean value indicating whether a password has been generated for
		 * the user.
		 * @return true if a password has been generated, false otherwise
		 */
		public boolean isPasswordGenerated() {
			return this.passwordGenerated;
		}

	}

}
