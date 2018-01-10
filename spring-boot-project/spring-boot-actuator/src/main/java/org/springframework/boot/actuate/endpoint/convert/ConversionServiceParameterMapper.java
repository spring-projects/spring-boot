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

import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMappingException;
import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * {@link ParameterMapper} that uses a {@link ConversionService} to map parameter values
 * if necessary.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ConversionServiceParameterMapper implements ParameterMapper {

	private final ConversionService conversionService;

	public ConversionServiceParameterMapper() {
		this(createDefaultConversionService());
	}

	/**
	 * Create a new instance with the {@link ConversionService} to use.
	 * @param conversionService the conversion service
	 */
	public ConversionServiceParameterMapper(ConversionService conversionService) {
		this.conversionService = new BinderConversionService(conversionService);
	}

	@Override
	public <T> T mapParameter(Object input, Class<T> parameterType) {
		try {
			return this.conversionService.convert(input, parameterType);
		}
		catch (Exception ex) {
			throw new ParameterMappingException(input, parameterType, ex);
		}
	}

	private static ConversionService createDefaultConversionService() {
		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		IsoOffsetDateTimeConverter.registerConverter(conversionService);
		return conversionService;
	}

}
