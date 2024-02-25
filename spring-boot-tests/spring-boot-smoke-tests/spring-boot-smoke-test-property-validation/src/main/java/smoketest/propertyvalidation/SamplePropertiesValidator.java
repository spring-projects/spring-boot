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

package smoketest.propertyvalidation;

import java.util.regex.Pattern;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * SamplePropertiesValidator class.
 */
public class SamplePropertiesValidator implements Validator {

	final Pattern pattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

	/**
	 * Returns true if the specified type is supported by this validator.
	 * @param type the type to check
	 * @return true if the specified type is supported, false otherwise
	 */
	@Override
	public boolean supports(Class<?> type) {
		return type == SampleProperties.class;
	}

	/**
	 * Validates the given object using the provided Errors object.
	 * @param o the object to be validated
	 * @param errors the Errors object to store any validation errors
	 */
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
