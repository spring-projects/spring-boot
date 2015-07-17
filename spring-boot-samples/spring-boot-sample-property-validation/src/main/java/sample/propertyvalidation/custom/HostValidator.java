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

package sample.propertyvalidation.custom;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class HostValidator implements ConstraintValidator<Host, String> {

	public static final String IP_REGEX = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";

	final Pattern pattern = Pattern.compile(IP_REGEX);

	@Override
	public void initialize(Host host) {
	}

	@Override
	public boolean isValid(String s,
			ConstraintValidatorContext constraintValidatorContext) {
		return pattern.matcher(s).matches();
	}
}
