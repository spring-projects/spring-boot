/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Used for testing validation of properties that have default field values.
 *
 * @author Madhura Bhave
 */
@EnableConfigurationProperties
@ConfigurationProperties
class ValidatorPropertiesWithDefaultValues implements Validator {

	private String bar = "a";

	@Override
	public boolean supports(Class<?> type) {
		return type == ValidatorPropertiesWithDefaultValues.class;
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "bar", "foo.empty");
	}

	public String getBar() {
		return this.bar;
	}

	public void setBar(String bar) {
		this.bar = bar;
	}

}
