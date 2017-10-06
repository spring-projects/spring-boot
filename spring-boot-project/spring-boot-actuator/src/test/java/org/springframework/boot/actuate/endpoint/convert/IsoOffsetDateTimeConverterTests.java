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

package org.springframework.boot.actuate.endpoint.convert;

import java.util.Date;

import org.junit.Test;

import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IsoOffsetDateTimeConverter}.
 *
 * @author Phillip Webb
 */
public class IsoOffsetDateTimeConverterTests {

	@Test
	public void convertShouldConvertIsoDate() throws Exception {
		IsoOffsetDateTimeConverter converter = new IsoOffsetDateTimeConverter();
		Date date = converter.convert("2011-12-03T10:15:30+01:00");
		assertThat(date).isNotNull();
	}

	@Test
	public void registerConverterShouldRegister() throws Exception {
		DefaultConversionService service = new DefaultConversionService();
		IsoOffsetDateTimeConverter.registerConverter(service);
		Date date = service.convert("2011-12-03T10:15:30+01:00", Date.class);
		assertThat(date).isNotNull();
	}

}
