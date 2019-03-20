/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.Locale;

import org.junit.Test;

import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RelaxedConversionService}.
 *
 * @author Madhura Bhave
 */
public class RelaxedConversionServiceTests {

	private RelaxedConversionService conversionService = new RelaxedConversionService(
			new DefaultConversionService());

	@Test
	public void conversionServiceShouldAlwaysUseLocaleEnglish() {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("tr"));
			TestEnum result = this.conversionService
					.convert("accept-case-insensitive-properties", TestEnum.class);
			assertThat(result).isEqualTo(TestEnum.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	enum TestEnum {

		ACCEPT_CASE_INSENSITIVE_PROPERTIES

	}

}
