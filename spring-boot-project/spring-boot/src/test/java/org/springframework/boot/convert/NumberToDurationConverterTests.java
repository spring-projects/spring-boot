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

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NumberToDurationConverter}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class NumberToDurationConverterTests {

	private final ConversionService conversionService;

	public NumberToDurationConverterTests(String name, ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWhenSimpleWithoutSuffixShouldReturnDuration() {
		assertThat(convert(10)).isEqualTo(Duration.ofMillis(10));
		assertThat(convert(+10)).isEqualTo(Duration.ofMillis(10));
		assertThat(convert(-10)).isEqualTo(Duration.ofMillis(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDuration() {
		assertThat(convert(10, ChronoUnit.SECONDS)).isEqualTo(Duration.ofSeconds(10));
		assertThat(convert(+10, ChronoUnit.SECONDS)).isEqualTo(Duration.ofSeconds(10));
		assertThat(convert(-10, ChronoUnit.SECONDS)).isEqualTo(Duration.ofSeconds(-10));
	}

	private Duration convert(Integer source) {
		return this.conversionService.convert(source, Duration.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Duration convert(Integer source, ChronoUnit defaultUnit) {
		TypeDescriptor targetType = mock(TypeDescriptor.class);
		if (defaultUnit != null) {
			DurationUnit unitAnnotation = AnnotationUtils
					.synthesizeAnnotation(Collections.singletonMap("value", defaultUnit), DurationUnit.class, null);
			given(targetType.getAnnotation(DurationUnit.class)).willReturn(unitAnnotation);
		}
		given(targetType.getType()).willReturn((Class) Duration.class);
		return (Duration) this.conversionService.convert(source, TypeDescriptor.forObject(source), targetType);
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new NumberToDurationConverter());
	}

}
