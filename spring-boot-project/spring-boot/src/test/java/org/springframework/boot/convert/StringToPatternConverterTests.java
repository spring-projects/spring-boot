
/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.core.convert.ConversionService;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToPatternConverter}.
 *
 * @author Mikhael Sokolov
 */
class StringToPatternConverterTests {

	@ConversionServiceTest
	void convertWhenStringShouldReturnPattern(ConversionService conversionService) {
		assertThat(convert(conversionService, "([A-Z])\\w+").pattern()).isEqualTo(Pattern.compile("([A-Z])\\w+", Pattern.MULTILINE).pattern());
		assertThat(convert(conversionService, "(\\\\W)*(\\\\S)*").pattern()).isEqualTo(Pattern.compile("(\\\\W)*(\\\\S)*", Pattern.MULTILINE).pattern());
	}

	@ConversionServiceTest
	void convertWhenEmptyShouldReturnNull(ConversionService conversionService) {
		assertThat(convert(conversionService, "")).isNull();
	}

	private Pattern convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, Pattern.class);
	}

	@SuppressWarnings("unused")
	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new StringToPatternConverter());
	}
}
