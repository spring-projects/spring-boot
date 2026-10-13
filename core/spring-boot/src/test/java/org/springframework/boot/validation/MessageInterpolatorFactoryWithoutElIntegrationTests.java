/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link MessageInterpolatorFactory} without EL.
 *
 * @author Phillip Webb
 */
@ClassPathExclusions("tomcat-embed-el-*.jar")
class MessageInterpolatorFactoryWithoutElIntegrationTests {

	@Test
	void defaultMessageInterpolatorShouldFail() {
		// Sanity test
		assertThatExceptionOfType(ValidationException.class)
			.isThrownBy(Validation.byDefaultProvider().configure()::getDefaultMessageInterpolator)
			.withMessageContaining("jakarta.el.ExpressionFactory");
	}

	@Test
	void getObjectShouldUseFallback() {
		MessageInterpolator interpolator = new MessageInterpolatorFactory().getObject();
		assertThat(interpolator).isInstanceOf(ParameterMessageInterpolator.class);
	}

	@Test
	void getObjectShouldUseMessageSourceMessageInterpolatorDelegateWithFallback() {
		MessageSource messageSource = mock(MessageSource.class);
		MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(messageSource);
		MessageInterpolator interpolator = interpolatorFactory.getObject();
		assertThat(interpolator).isInstanceOf(MessageSourceMessageInterpolator.class);
		assertThat(interpolator).hasFieldOrPropertyWithValue("messageSource", messageSource);
		assertThat(ReflectionTestUtils.getField(interpolator, "messageInterpolator"))
			.isInstanceOf(ParameterMessageInterpolator.class);
	}

}
