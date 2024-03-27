/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.scheduling.support.CronExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * Tests for {@link StringToCronExpressionConverter}.
 *
 * @author Ahmad Amiri
 */
class StringToCronExpressionConverterTest {

	@ConversionServiceTest
	void isValidExpression(ConversionService conversionService) {
		assertThat(convert(conversionService, null)).isNull();
		assertThat(convert(conversionService, "")).isNull();
		assertThatThrownBy(() -> convert(conversionService, "*")).
			isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Cron expression must consist of 6 fields");
		assertThat(convert(conversionService, "* * * * * *")).isInstanceOf(CronExpression.class).isNotNull();
	}

	private CronExpression convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, CronExpression.class);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
			.with((conversionService) -> conversionService.addConverter(new StringToCronExpressionConverter()));
	}

}
