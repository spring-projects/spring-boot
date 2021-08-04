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

package org.springframework.boot.validation;

import javax.validation.MessageInterpolator;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.junit.jupiter.api.Test;

import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MessageInterpolatorFactory}.
 *
 * @author Phillip Webb
 */
class MessageInterpolatorFactoryTests {

	@Test
	void getObjectShouldReturnResourceBundleMessageInterpolator() {
		MessageInterpolator interpolator = new MessageInterpolatorFactory().getObject();
		assertThat(interpolator).isInstanceOf(ResourceBundleMessageInterpolator.class);
	}

	@Test
	void getObjectShouldReturnMessageSourceMessageInterpolatorDelegateWithResourceBundleMessageInterpolator() {
		MessageSource messageSource = mock(MessageSource.class);
		MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(messageSource);
		MessageInterpolator interpolator = interpolatorFactory.getObject();
		assertThat(interpolator).isInstanceOf(MessageSourceMessageInterpolator.class);
		assertThat(interpolator).hasFieldOrPropertyWithValue("messageSource", messageSource);
		assertThat(ReflectionTestUtils.getField(interpolator, "messageInterpolator"))
				.isInstanceOf(ResourceBundleMessageInterpolator.class);
	}

}
