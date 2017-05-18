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

import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

/**
 * Tests for the {@link EnvironmentConverter} methods
 *
 * @author Ethan Rubinson
 */
public class EnvironmentConverterTest {

	@Test
	public void testConvertAbstractEnvironmentToStandardEnvironment() throws Exception {
		final AbstractEnvironment baseEnv = new MockEnvironment();
		final CustomConversionService customConverterServce = new CustomConversionService(
				baseEnv.getConversionService());
		final String[] activeProfiles = new String[] { "activeProfile1", "activeProfile2" };
		baseEnv.setActiveProfiles(activeProfiles);
		baseEnv.setConversionService(customConverterServce);

		ConfigurableEnvironment convertedEnv = EnvironmentConverter
				.convertToStandardEnvironment(baseEnv);
		Assert.assertTrue(Arrays.areEqual(activeProfiles,
				convertedEnv.getActiveProfiles()));
		Assert.assertEquals(customConverterServce, convertedEnv.getConversionService());
	}

	private class CustomConversionService implements ConfigurableConversionService {

		private final ConfigurableConversionService delegate;

		CustomConversionService(ConfigurableConversionService delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
			return this.delegate.canConvert(sourceType, targetType);
		}

		@Override
		public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return this.delegate.canConvert(sourceType, targetType);
		}

		@Override
		public <T> T convert(Object source, Class<T> targetType) {
			return this.delegate.convert(source, targetType);
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			return this.delegate.convert(source, sourceType, targetType);
		}

		@Override
		public void addConverter(Converter<?, ?> converter) {
			this.delegate.addConverter(converter);
		}

		@Override
		public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType,
				Converter<? super S, ? extends T> converter) {
			this.delegate.addConverter(sourceType, targetType, converter);
		}

		@Override
		public void addConverter(GenericConverter converter) {
			this.delegate.addConverter(converter);
		}

		@Override
		public void addConverterFactory(ConverterFactory<?, ?> factory) {
			this.delegate.addConverterFactory(factory);
		}

		@Override
		public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
			this.delegate.removeConvertible(sourceType, targetType);
		}

	}

}
