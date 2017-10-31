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

package org.springframework.boot.context.properties.bind.convert;

import java.util.function.Function;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * {@link ConversionService} used by the {@link Binder}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class BinderConversionService implements ConversionService {

	private final ConversionService conversionService;

	private final ConversionService additionalConversionService;

	/**
	 * Create a new {@link BinderConversionService} instance.
	 * @param conversionService and option root conversion service
	 */
	public BinderConversionService(ConversionService conversionService) {
		this.conversionService = (conversionService != null ? conversionService
				: new DefaultFormattingConversionService());
		this.additionalConversionService = createAdditionalConversionService();
	}

	@Override
	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return (this.conversionService != null
				&& this.conversionService.canConvert(sourceType, targetType))
				|| this.additionalConversionService.canConvert(sourceType, targetType);
	}

	@Override
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (this.conversionService != null
				&& this.conversionService.canConvert(sourceType, targetType))
				|| this.additionalConversionService.canConvert(sourceType, targetType);
	}

	@Override
	public <T> T convert(Object source, Class<T> targetType) {
		return callConversionService((c) -> c.convert(source, targetType));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		return callConversionService((c) -> c.convert(source, sourceType, targetType));
	}

	private <T> T callConversionService(Function<ConversionService, T> call) {
		if (this.conversionService == null) {
			return callAdditionalConversionService(call, null);
		}
		try {
			return call.apply(this.conversionService);
		}
		catch (ConversionException ex) {
			return callAdditionalConversionService(call, ex);
		}
	}

	private <T> T callAdditionalConversionService(Function<ConversionService, T> call,
			RuntimeException cause) {
		try {
			return call.apply(this.additionalConversionService);
		}
		catch (ConverterNotFoundException ex) {
			throw (cause != null ? cause : ex);
		}
	}

	private static ConversionService createAdditionalConversionService() {
		DefaultFormattingConversionService service = new DefaultFormattingConversionService();
		DefaultConversionService.addCollectionConverters(service);
		service.addConverterFactory(new StringToEnumConverterFactory());
		service.addConverter(new StringToCharArrayConverter());
		service.addConverter(new StringToInetAddressConverter());
		service.addConverter(new InetAddressToStringConverter());
		service.addConverter(new PropertyEditorConverter());
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		DateFormatter formatter = new DateFormatter();
		formatter.setIso(DateTimeFormat.ISO.DATE_TIME);
		registrar.setFormatter(formatter);
		registrar.registerFormatters(service);
		return service;
	}

}
