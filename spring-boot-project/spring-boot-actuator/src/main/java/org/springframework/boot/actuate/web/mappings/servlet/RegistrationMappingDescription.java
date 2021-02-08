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

package org.springframework.boot.actuate.web.mappings.servlet;

import javax.servlet.Registration;

/**
 * A mapping description derived from a {@link Registration}.
 *
 * @param <T> type of the registration
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class RegistrationMappingDescription<T extends Registration> {

	private final T registration;

	/**
	 * Creates a new {@link RegistrationMappingDescription} derived from the given
	 * {@code registration} and with the given {@code predicate}.
	 * @param registration the registration
	 */
	public RegistrationMappingDescription(T registration) {
		this.registration = registration;
	}

	/**
	 * Returns the name of the registered Filter or Servlet.
	 * @return the name
	 */
	public String getName() {
		return this.registration.getName();
	}

	/**
	 * Returns the class name of the registered Filter or Servlet.
	 * @return the class name
	 */
	public String getClassName() {
		return this.registration.getClassName();
	}

	/**
	 * Returns the registration that is being described.
	 * @return the registration
	 */
	protected final T getRegistration() {
		return this.registration;
	}

}
