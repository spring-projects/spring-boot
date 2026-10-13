/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation;

import jakarta.validation.MessageInterpolator;
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
