/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.mappings;

import jakarta.servlet.Registration;

/**
 * A mapping description derived from a {@link Registration}.
 *
 * @param <T> type of the registration
 * @author Andy Wilkinson
 * @since 4.0.0
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
