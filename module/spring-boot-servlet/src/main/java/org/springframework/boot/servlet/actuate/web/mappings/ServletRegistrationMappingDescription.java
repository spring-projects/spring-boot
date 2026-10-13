/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.mappings;

import java.util.Collection;

import jakarta.servlet.ServletRegistration;

/**
 * A mapping description derived from a {@link ServletRegistration}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class ServletRegistrationMappingDescription extends RegistrationMappingDescription<ServletRegistration> {

	/**
	 * Creates a new {@code ServletRegistrationMappingDescription} derived from the given
	 * {@code servletRegistration}.
	 * @param servletRegistration the servlet registration
	 */
	public ServletRegistrationMappingDescription(ServletRegistration servletRegistration) {
		super(servletRegistration);
	}

	/**
	 * Returns the mappings for the registered servlet.
	 * @return the mappings
	 */
	public Collection<String> getMappings() {
		return getRegistration().getMappings();
	}

}
