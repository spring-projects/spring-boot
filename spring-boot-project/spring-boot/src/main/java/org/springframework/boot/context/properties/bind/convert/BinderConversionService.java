/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
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

	private static final ConversionService defaultConversionService = new DefaultFormattingConversionService();

	private final List<ConversionService> conversionServices;

	/**
	 * Create a new {@link BinderConversionService} instance.
	 * @param conversionService and option root conversion service
	 */
	public BinderConversionService(ConversionService conversionService) {
		List<ConversionService> conversionServices = new ArrayList<>();
		conversionServices.add(createOverrideConversionService());
		conversionServices.add(
				conversionService != null ? conversionService : defaultConversionService);
		conversionServices.add(createAdditionalConversionService());
		this.conversionServices = Collections.unmodifiableList(conversionServices);
	}

	private ConversionService createOverrideConversionService() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new DelimitedStringToCollectionConverter(this));
		return service;
	}

	private ConversionService createAdditionalConversionService() {
		DefaultFormattingConversionService service = new DefaultFormattingConversionService();
		DefaultConversionService.addCollectionConverters(service);
		service.addConverterFactory(new StringToEnumConverterFactory());
		service.addConverter(new StringToCharArrayConverter());
		service.addConverter(new StringToInetAddressConverter());
		service.addConverter(new InetAddressToStringConverter());
		service.addConverter(new PropertyEditorConverter());
		service.addConverter(new DurationConverter());
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		DateFormatter formatter = new DateFormatter();
		formatter.setIso(DateTimeFormat.ISO.DATE_TIME);
		registrar.setFormatter(formatter);
		registrar.registerFormatters(service);
		return service;
	}

	@Override
	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		for (ConversionService service : this.conversionServices) {
			if (service.canConvert(sourceType, targetType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		for (ConversionService service : this.conversionServices) {
			if (service.canConvert(sourceType, targetType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T> T convert(Object source, Class<T> targetType) {
		return callConversionServices((c) -> c.convert(source, targetType));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		return callConversionServices((c) -> c.convert(source, sourceType, targetType));
	}

	private <T> T callConversionServices(Function<ConversionService, T> call) {
		ConversionException exception = null;
		for (ConversionService service : this.conversionServices) {
			try {
				return call.apply(service);
			}
			catch (ConversionException ex) {
				exception = ex;
			}
		}
		throw exception;
	}

}
