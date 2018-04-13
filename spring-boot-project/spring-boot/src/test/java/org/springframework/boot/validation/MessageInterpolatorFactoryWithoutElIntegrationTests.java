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

package org.springframework.boot.validation;

import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidationException;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MessageInterpolatorFactory} without EL.
 *
 * @author Phillip Webb
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("tomcat-embed-el-*.jar")
public class MessageInterpolatorFactoryWithoutElIntegrationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void defaultMessageInterpolatorShouldFail() {
		// Sanity test
		this.thrown.expect(ValidationException.class);
		this.thrown.expectMessage("javax.el.ExpressionFactory");
		Validation.byDefaultProvider().configure().getDefaultMessageInterpolator();
	}

	@Test
	public void getObjectShouldUseFallback() {
		MessageInterpolator interpolator = new MessageInterpolatorFactory().getObject();
		assertThat(interpolator).isInstanceOf(ParameterMessageInterpolator.class);
	}

}
