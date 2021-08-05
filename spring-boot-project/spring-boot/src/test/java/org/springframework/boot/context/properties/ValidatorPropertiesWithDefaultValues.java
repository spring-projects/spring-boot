/*
 * Copyright 2012-2021 the original author or authors.
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
