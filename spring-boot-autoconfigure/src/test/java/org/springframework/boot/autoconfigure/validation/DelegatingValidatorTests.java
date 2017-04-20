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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DelegatingValidator}.
 *
 * @author Phillip Webb
 */
public class DelegatingValidatorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private SmartValidator delegate;

	private DelegatingValidator delegating;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.delegating = new DelegatingValidator(this.delegate);
	}

	@Test
	public void createWhenJsrValidatorIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Target Validator must not be null");
		new DelegatingValidator((javax.validation.Validator) null);
	}

	@Test
	public void createWithJsrValidatorShouldAdapt() throws Exception {
		javax.validation.Validator delegate = mock(javax.validation.Validator.class);
		Validator delegating = new DelegatingValidator(delegate);
		Object target = new Object();
		Errors errors = new BeanPropertyBindingResult(target, "foo");
		delegating.validate(target, errors);
		verify(delegate).validate(any());
	}

	@Test
	public void createWithSpringValidatorWhenValidatorIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Target Validator must not be null");
		new DelegatingValidator((Validator) null);
	}

	@Test
	public void supportsShouldDelegateToValidator() throws Exception {
		this.delegating.supports(Object.class);
		verify(this.delegate).supports(Object.class);
	}

	@Test
	public void validateShouldDelegateToValidator() throws Exception {
		Object target = new Object();
		Errors errors = new BeanPropertyBindingResult(target, "foo");
		this.delegating.validate(target, errors);
		verify(this.delegate).validate(target, errors);
	}

	@Test
	public void validateWithHintsShouldDelegateToValidator() throws Exception {
		Object target = new Object();
		Errors errors = new BeanPropertyBindingResult(target, "foo");
		Object[] hints = { "foo", "bar" };
		this.delegating.validate(target, errors, hints);
		verify(this.delegate).validate(target, errors, hints);
	}

	@Test
	public void validateWithHintsWhenDelegateIsNotSmartShouldDelegateToSimpleValidator()
			throws Exception {
		Validator delegate = mock(Validator.class);
		DelegatingValidator delegating = new DelegatingValidator(delegate);
		Object target = new Object();
		Errors errors = new BeanPropertyBindingResult(target, "foo");
		Object[] hints = { "foo", "bar" };
		delegating.validate(target, errors, hints);
		verify(delegate).validate(target, errors);
	}

}
