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

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.StringValueResolver;

/**
 * A specialization of {@link FormattingConversionService} configured by default with
 * converters and formatters appropriate for most Spring Boot applications.
 * <p>
 * Designed for direct instantiation but also exposes the static
 * {@link #addApplicationConverters} and
 * {@link #addApplicationFormatters(FormatterRegistry)} utility methods for ad-hoc use
 * against registry instance.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationConversionService extends FormattingConversionService {

	private static volatile ApplicationConversionService sharedInstance;

	private final boolean unmodifiable;

	public ApplicationConversionService() {
		this(null);
	}

	public ApplicationConversionService(StringValueResolver embeddedValueResolver) {
		this(embeddedValueResolver, false);
	}

	private ApplicationConversionService(StringValueResolver embeddedValueResolver, boolean unmodifiable) {
		if (embeddedValueResolver != null) {
			setEmbeddedValueResolver(embeddedValueResolver);
		}
		configure(this);
		this.unmodifiable = unmodifiable;
	}

	@Override
	public void addPrinter(Printer<?> printer) {
		assertModifiable();
		super.addPrinter(printer);
	}

	@Override
	public void addParser(Parser<?> parser) {
		assertModifiable();
		super.addParser(parser);
	}

	@Override
	public void addFormatter(Formatter<?> formatter) {
		assertModifiable();
		super.addFormatter(formatter);
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
		assertModifiable();
		super.addFormatterForFieldType(fieldType, formatter);
	}

	@Override
	public void addConverter(Converter<?, ?> converter) {
		assertModifiable();
		super.addConverter(converter);
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
		assertModifiable();
		super.addFormatterForFieldType(fieldType, printer, parser);
	}

	@Override
	public void addFormatterForFieldAnnotation(
			AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory) {
		assertModifiable();
		super.addFormatterForFieldAnnotation(annotationFormatterFactory);
	}

	@Override
	public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType,
			Converter<? super S, ? extends T> converter) {
		assertModifiable();
		super.addConverter(sourceType, targetType, converter);
	}

	@Override
	public void addConverter(GenericConverter converter) {
		assertModifiable();
		super.addConverter(converter);
	}

	@Override
	public void addConverterFactory(ConverterFactory<?, ?> factory) {
		assertModifiable();
		super.addConverterFactory(factory);
	}

	@Override
	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		assertModifiable();
		super.removeConvertible(sourceType, targetType);
	}

	private void assertModifiable() {
		if (this.unmodifiable) {
			throw new UnsupportedOperationException("This ApplicationConversionService cannot be modified");
		}
	}

	/**
	 * Return {@code true} if objects of {@code sourceType} can be converted to the
	 * {@code targetType} and the converter has {@code Object.class} as a supported source
	 * type.
	 * @param sourceType the source type to test
	 * @param targetType the target type to test
	 * @return if conversion happens through an {@code ObjectTo...} converter
	 * @since 2.4.3
	 */
	public boolean isConvertViaObjectSourceType(TypeDescriptor sourceType, TypeDescriptor targetType) {
		GenericConverter converter = getConverter(sourceType, targetType);
		Set<ConvertiblePair> pairs = (converter != null) ? converter.getConvertibleTypes() : null;
		if (pairs != null) {
			for (ConvertiblePair pair : pairs) {
				if (Object.class.equals(pair.getSourceType())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return a shared default application {@code ConversionService} instance, lazily
	 * building it once needed.
	 * <p>
	 * Note: This method actually returns an {@link ApplicationConversionService}
	 * instance. However, the {@code ConversionService} signature has been preserved for
	 * binary compatibility.
	 * @return the shared {@code ApplicationConversionService} instance (never
	 * {@code null})
	 */
	public static ConversionService getSharedInstance() {
		ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
		if (sharedInstance == null) {
			synchronized (ApplicationConversionService.class) {
				sharedInstance = ApplicationConversionService.sharedInstance;
				if (sharedInstance == null) {
					sharedInstance = new ApplicationConversionService(null, true);
					ApplicationConversionService.sharedInstance = sharedInstance;
				}
			}
		}
		return sharedInstance;
	}

	/**
	 * Configure the given {@link FormatterRegistry} with formatters and converters
	 * appropriate for most Spring Boot applications.
	 * @param registry the registry of converters to add to (must also be castable to
	 * ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given FormatterRegistry could not be cast to a
	 * ConversionService
	 */
	public static void configure(FormatterRegistry registry) {
		DefaultConversionService.addDefaultConverters(registry);
		DefaultFormattingConversionService.addDefaultFormatters(registry);
		addApplicationFormatters(registry);
		addApplicationConverters(registry);
	}

	/**
	 * Add converters useful for most Spring Boot applications.
	 * @param registry the registry of converters to add to (must also be castable to
	 * ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a
	 * ConversionService
	 */
	public static void addApplicationConverters(ConverterRegistry registry) {
		addDelimitedStringConverters(registry);
		registry.addConverter(new StringToDurationConverter());
		registry.addConverter(new DurationToStringConverter());
		registry.addConverter(new NumberToDurationConverter());
		registry.addConverter(new DurationToNumberConverter());
		registry.addConverter(new StringToPeriodConverter());
		registry.addConverter(new PeriodToStringConverter());
		registry.addConverter(new NumberToPeriodConverter());
		registry.addConverter(new StringToDataSizeConverter());
		registry.addConverter(new NumberToDataSizeConverter());
		registry.addConverter(new StringToFileConverter());
		registry.addConverter(new InputStreamSourceToByteArrayConverter());
		registry.addConverterFactory(new LenientStringToEnumConverterFactory());
		registry.addConverterFactory(new LenientBooleanToEnumConverterFactory());
		if (registry instanceof ConversionService) {
			addApplicationConverters(registry, (ConversionService) registry);
		}
	}

	private static void addApplicationConverters(ConverterRegistry registry, ConversionService conversionService) {
		registry.addConverter(new CharSequenceToObjectConverter(conversionService));
	}

	/**
	 * Add converters to support delimited strings.
	 * @param registry the registry of converters to add to (must also be castable to
	 * ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a
	 * ConversionService
	 */
	public static void addDelimitedStringConverters(ConverterRegistry registry) {
		ConversionService service = (ConversionService) registry;
		registry.addConverter(new ArrayToDelimitedStringConverter(service));
		registry.addConverter(new CollectionToDelimitedStringConverter(service));
		registry.addConverter(new DelimitedStringToArrayConverter(service));
		registry.addConverter(new DelimitedStringToCollectionConverter(service));
	}

	/**
	 * Add formatters useful for most Spring Boot applications.
	 * @param registry the service to register default formatters with
	 */
	public static void addApplicationFormatters(FormatterRegistry registry) {
		registry.addFormatter(new CharArrayFormatter());
		registry.addFormatter(new InetAddressFormatter());
		registry.addFormatter(new IsoOffsetFormatter());
	}

	/**
	 * Add {@link GenericConverter}, {@link Converter}, {@link Printer}, {@link Parser}
	 * and {@link Formatter} beans from the specified context.
	 * @param registry the service to register beans with
	 * @param beanFactory the bean factory to get the beans from
	 * @since 2.2.0
	 */
	public static void addBeans(FormatterRegistry registry, ListableBeanFactory beanFactory) {
		Set<Object> beans = new LinkedHashSet<>();
		beans.addAll(beanFactory.getBeansOfType(GenericConverter.class).values());
		beans.addAll(beanFactory.getBeansOfType(Converter.class).values());
		beans.addAll(beanFactory.getBeansOfType(Printer.class).values());
		beans.addAll(beanFactory.getBeansOfType(Parser.class).values());
		for (Object bean : beans) {
			if (bean instanceof GenericConverter) {
				registry.addConverter((GenericConverter) bean);
			}
			else if (bean instanceof Converter) {
				registry.addConverter((Converter<?, ?>) bean);
			}
			else if (bean instanceof Formatter) {
				registry.addFormatter((Formatter<?>) bean);
			}
			else if (bean instanceof Printer) {
				registry.addPrinter((Printer<?>) bean);
			}
			else if (bean instanceof Parser) {
				registry.addParser((Parser<?>) bean);
			}
		}
	}

}
