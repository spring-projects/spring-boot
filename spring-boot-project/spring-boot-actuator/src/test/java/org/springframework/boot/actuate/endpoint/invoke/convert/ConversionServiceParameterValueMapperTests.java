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

package org.springframework.boot.actuate.endpoint.invoke.convert;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.ParameterMappingException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConversionServiceParameterValueMapper}.
 *
 * @author Phillip Webb
 */
class ConversionServiceParameterValueMapperTests {

	@Test
	void mapParameterShouldDelegateToConversionService() {
		DefaultFormattingConversionService conversionService = spy(new DefaultFormattingConversionService());
		ConversionServiceParameterValueMapper mapper = new ConversionServiceParameterValueMapper(conversionService);
		Object mapped = mapper.mapParameterValue(new TestOperationParameter(Integer.class), "123");
		assertThat(mapped).isEqualTo(123);
		verify(conversionService).convert("123", Integer.class);
	}

	@Test
	void mapParameterWhenConversionServiceFailsShouldThrowParameterMappingException() {
		ConversionService conversionService = mock(ConversionService.class);
		RuntimeException error = new RuntimeException();
		given(conversionService.convert(any(), any())).willThrow(error);
		ConversionServiceParameterValueMapper mapper = new ConversionServiceParameterValueMapper(conversionService);
		assertThatExceptionOfType(ParameterMappingException.class)
				.isThrownBy(() -> mapper.mapParameterValue(new TestOperationParameter(Integer.class), "123"))
				.satisfies((ex) -> {
					assertThat(ex.getValue()).isEqualTo("123");
					assertThat(ex.getParameter().getType()).isEqualTo(Integer.class);
					assertThat(ex.getCause()).isEqualTo(error);
				});
	}

	@Test
	void createShouldRegisterIsoOffsetDateTimeConverter() {
		ConversionServiceParameterValueMapper mapper = new ConversionServiceParameterValueMapper();
		Object mapped = mapper.mapParameterValue(new TestOperationParameter(OffsetDateTime.class),
				"2011-12-03T10:15:30+01:00");
		assertThat(mapped).isNotNull();
	}

	@Test
	void createWithConversionServiceShouldNotRegisterIsoOffsetDateTimeConverter() {
		ConversionService conversionService = new DefaultConversionService();
		ConversionServiceParameterValueMapper mapper = new ConversionServiceParameterValueMapper(conversionService);
		assertThatExceptionOfType(ParameterMappingException.class).isThrownBy(() -> mapper
				.mapParameterValue(new TestOperationParameter(OffsetDateTime.class), "2011-12-03T10:15:30+01:00"));
	}

	static class TestOperationParameter implements OperationParameter {

		private final Class<?> type;

		TestOperationParameter(Class<?> type) {
			this.type = type;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public Class<?> getType() {
			return this.type;
		}

		@Override
		public boolean isMandatory() {
			return false;
		}

	}

}
