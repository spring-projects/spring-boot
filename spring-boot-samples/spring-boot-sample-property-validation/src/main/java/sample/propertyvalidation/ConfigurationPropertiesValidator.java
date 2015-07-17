/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.propertyvalidation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;

@Component(value = "configurationPropertiesValidator")
public class ConfigurationPropertiesValidator implements Validator {

	public static final String IP_REGEX = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";

	final Pattern pattern = Pattern.compile(IP_REGEX);

	@Override public boolean supports(Class<?> aClass) {
		return SampleProperties.class.equals(aClass);
	}

	@Override public void validate(Object o, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "host", "host.empty");
		ValidationUtils.rejectIfEmpty(errors, "port", "port.empty");

		SampleProperties properties = (SampleProperties) o;
		if(!pattern.matcher(properties.getHost()).matches()) {
			errors.rejectValue("host", "Invalid host");
		}
	}

}
