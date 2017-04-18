/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.validation;

import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * {@link Validator} implementation that delegates calls to another {@link Validator}.
 * This {@link Validator} implements Spring's {@link SmartValidator} interface but does
 * not implement the JSR-303 {@code javax.validator.Validator} interface.
 *
 * @author Phillip Webb
 * @since 1.5.3
 */
public class DelegatingValidator implements SmartValidator {

	private final Validator delegate;

	/**
	 * Create a new {@link DelegatingValidator} instance.
	 * @param targetValidator the target JSR validator
	 */
	public DelegatingValidator(javax.validation.Validator targetValidator) {
		this.delegate = new SpringValidatorAdapter(targetValidator);
	}

	/**
	 * Create a new {@link DelegatingValidator} instance.
	 * @param targetValidator the target validator
	 */
	public DelegatingValidator(Validator targetValidator) {
		Assert.notNull(targetValidator, "Target Validator must not be null");
		this.delegate = targetValidator;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return this.delegate.supports(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		this.delegate.validate(target, errors);
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		if (this.delegate instanceof SmartValidator) {
			((SmartValidator) this.delegate).validate(target, errors, validationHints);
		}
		else {
			this.delegate.validate(target, errors);
		}
	}

	protected final Validator getDelegate() {
		return this.delegate;
	}

}
