/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.mappings;

import java.util.Collection;

import jakarta.servlet.FilterRegistration;

/**
 * A {@link RegistrationMappingDescription} derived from a {@link FilterRegistration}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class FilterRegistrationMappingDescription extends RegistrationMappingDescription<FilterRegistration> {

	/**
	 * Creates a new {@code FilterRegistrationMappingDescription} derived from the given
	 * {@code filterRegistration}.
	 * @param filterRegistration the filter registration
	 */
	public FilterRegistrationMappingDescription(FilterRegistration filterRegistration) {
		super(filterRegistration);
	}

	/**
	 * Returns the servlet name mappings for the registered filter.
	 * @return the mappings
	 */
	public Collection<String> getServletNameMappings() {
		return getRegistration().getServletNameMappings();
	}

	/**
	 * Returns the URL pattern mappings for the registered filter.
	 * @return the mappings
	 */
	public Collection<String> getUrlPatternMappings() {
		return getRegistration().getUrlPatternMappings();
	}

}
