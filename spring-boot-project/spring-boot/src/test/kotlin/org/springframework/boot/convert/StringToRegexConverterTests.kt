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
package org.springframework.boot.convert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.provider.Arguments
import org.springframework.core.convert.ConversionService
import java.util.stream.Stream

/**
 * Tests for [StringToRegexConverter].
 *
 * @author Mikhael Sokolov
 */
internal class StringToRegexConverterTests {

	@ConversionServiceTest
	fun convertWhenStringShouldReturnPattern(conversionService: ConversionService) {
		assertThat(conversionService.convert("([A-Z])\\w+")?.pattern).isEqualTo(Regex("([A-Z])\\w+", RegexOption.MULTILINE).pattern)
		assertThat(conversionService.convert("(\\\\W)*(\\\\S)*")?.pattern).isEqualTo(Regex("(\\\\W)*(\\\\S)*", RegexOption.MULTILINE).pattern)
	}

	@ConversionServiceTest
	fun convertWhenEmptyShouldReturnNull(conversionService: ConversionService) {
		assertThat(conversionService.convert("")).isNull()
	}

	private fun ConversionService.convert(source: String): Regex? = convert(source, Regex::class.java)

	companion object {
		@JvmStatic
		fun conversionServices(): Stream<out Arguments?> = ConversionServiceArguments.with(StringToRegexConverter)
	}
}