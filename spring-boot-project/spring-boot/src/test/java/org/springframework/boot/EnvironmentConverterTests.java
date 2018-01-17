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

package org.springframework.boot;

import org.junit.Test;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EnvironmentConverter}.
 *
 * @author Ethan Rubinson
 * @author Andy Wilkinson
 */
public class EnvironmentConverterTests {

	private final EnvironmentConverter environmentConverter = new EnvironmentConverter(
			getClass().getClassLoader());

	@Test
	public void convertedEnvironmentHasSameActiveProfiles() {
		AbstractEnvironment originalEnvironment = new MockEnvironment();
		originalEnvironment.setActiveProfiles("activeProfile1", "activeProfile2");
		StandardEnvironment convertedEnvironment = this.environmentConverter
				.convertToStandardEnvironmentIfNecessary(originalEnvironment);
		assertThat(convertedEnvironment.getActiveProfiles())
				.containsExactly("activeProfile1", "activeProfile2");
	}

	@Test
	public void convertedEnvironmentHasSameConversionService() {
		AbstractEnvironment originalEnvironment = new MockEnvironment();
		ConfigurableConversionService conversionService = mock(
				ConfigurableConversionService.class);
		originalEnvironment.setConversionService(conversionService);
		StandardEnvironment convertedEnvironment = this.environmentConverter
				.convertToStandardEnvironmentIfNecessary(originalEnvironment);
		assertThat(convertedEnvironment.getConversionService())
				.isEqualTo(conversionService);
	}

	@Test
	public void standardEnvironmentIsReturnedUnconverted() {
		StandardEnvironment standardEnvironment = new StandardEnvironment();
		StandardEnvironment convertedEnvironment = this.environmentConverter
				.convertToStandardEnvironmentIfNecessary(standardEnvironment);
		assertThat(convertedEnvironment).isSameAs(standardEnvironment);
	}

	@Test
	public void standardServletEnvironmentIsConverted() {
		StandardServletEnvironment standardServletEnvironment = new StandardServletEnvironment();
		StandardEnvironment convertedEnvironment = this.environmentConverter
				.convertToStandardEnvironmentIfNecessary(standardServletEnvironment);
		assertThat(convertedEnvironment).isNotSameAs(standardServletEnvironment);
	}

}
