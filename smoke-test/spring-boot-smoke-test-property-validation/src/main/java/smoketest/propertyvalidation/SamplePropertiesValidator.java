/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.propertyvalidation;

import java.util.regex.Pattern;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class SamplePropertiesValidator implements Validator {

	final Pattern pattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

	@Override
	public boolean supports(Class<?> type) {
		return type == SampleProperties.class;
	}

	@Override
	public void validate(Object o, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "host", "host.empty");
		ValidationUtils.rejectIfEmpty(errors, "port", "port.empty");
		SampleProperties properties = (SampleProperties) o;
		if (properties.getHost() != null && !this.pattern.matcher(properties.getHost()).matches()) {
			errors.rejectValue("host", "Invalid host");
		}
	}

}
