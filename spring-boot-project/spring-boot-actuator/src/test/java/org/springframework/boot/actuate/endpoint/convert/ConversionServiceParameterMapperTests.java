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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.reflect.ParameterMappingException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConversionServiceParameterMapper}.
 *
 * @author Phillip Webb
 */
public class ConversionServiceParameterMapperTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void mapParameterShouldDelegateToConversionService() throws Exception {
		DefaultFormattingConversionService conversionService = spy(
				new DefaultFormattingConversionService());
		ConversionServiceParameterMapper mapper = new ConversionServiceParameterMapper(
				conversionService);
		Integer mapped = mapper.mapParameter("123", Integer.class);
		assertThat(mapped).isEqualTo(123);
		verify(conversionService).convert("123", Integer.class);
	}

	@Test
	public void mapParameterWhenConversionServiceFailsShouldThrowParameterMappingException()
			throws Exception {
		ConversionService conversionService = mock(ConversionService.class);
		RuntimeException error = new RuntimeException();
		given(conversionService.convert(any(), any())).willThrow(error);
		ConversionServiceParameterMapper mapper = new ConversionServiceParameterMapper(
				conversionService);
		try {
			mapper.mapParameter("123", Integer.class);
			fail("Did not throw");
		}
		catch (ParameterMappingException ex) {
			assertThat(ex.getInput()).isEqualTo("123");
			assertThat(ex.getType()).isEqualTo(Integer.class);
			assertThat(ex.getCause()).isEqualTo(error);
		}
	}

	@Test
	public void createShouldRegisterIsoOffsetDateTimeConverter() throws Exception {
		ConversionServiceParameterMapper mapper = new ConversionServiceParameterMapper();
		Date mapped = mapper.mapParameter("2011-12-03T10:15:30+01:00", Date.class);
		assertThat(mapped).isNotNull();
	}

	@Test
	public void createWithConversionServiceShouldNotRegisterIsoOffsetDateTimeConverter()
			throws Exception {
		ConversionService conversionService = new DefaultConversionService();
		ConversionServiceParameterMapper mapper = new ConversionServiceParameterMapper(
				conversionService);
		this.thrown.expect(ParameterMappingException.class);
		mapper.mapParameter("2011-12-03T10:15:30+01:00", Date.class);
	}

}
