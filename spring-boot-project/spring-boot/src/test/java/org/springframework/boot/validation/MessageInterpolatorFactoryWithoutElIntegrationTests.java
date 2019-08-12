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

package org.springframework.boot.validation;

import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidationException;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
				.withMessageContaining("javax.el.ExpressionFactory");
	}

	@Test
	void getObjectShouldUseFallback() {
		MessageInterpolator interpolator = new MessageInterpolatorFactory().getObject();
		assertThat(interpolator).isInstanceOf(ParameterMessageInterpolator.class);
	}

}
