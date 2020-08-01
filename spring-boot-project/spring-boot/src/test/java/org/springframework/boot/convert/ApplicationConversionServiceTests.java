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

package org.springframework.boot.convert;

import java.text.ParseException;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link ApplicationConversionService}.
 *
 * @author Phillip Webb
 */
class ApplicationConversionServiceTests {

	private FormatterRegistry registry = mock(FormatterRegistry.class);

	@Test
	void addBeansWhenHasGenericConverterBeanAddConverter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				ExampleGenericConverter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry).addConverter(context.getBean(ExampleGenericConverter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	void addBeansWhenHasConverterBeanAddConverter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleConverter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry).addConverter(context.getBean(ExampleConverter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	void addBeansWhenHasFormatterBeanAddsOnlyFormatter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleFormatter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry).addFormatter(context.getBean(ExampleFormatter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	void addBeansWhenHasPrinterBeanAddPrinter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExamplePrinter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry).addPrinter(context.getBean(ExamplePrinter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	void addBeansWhenHasParserBeanAddParser() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleParser.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry).addParser(context.getBean(ExampleParser.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	static class ExampleGenericConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}

	}

	static class ExampleConverter implements Converter<String, Integer> {

		@Override
		public Integer convert(String source) {
			return null;
		}

	}

	static class ExampleFormatter implements Formatter<Integer> {

		@Override
		public String print(Integer object, Locale locale) {
			return null;
		}

		@Override
		public Integer parse(String text, Locale locale) throws ParseException {
			return null;
		}

	}

	static class ExampleParser implements Parser<Integer> {

		@Override
		public Integer parse(String text, Locale locale) throws ParseException {
			return null;
		}

	}

	static class ExamplePrinter implements Printer<Integer> {

		@Override
		public String print(Integer object, Locale locale) {
			return null;
		}

	}

}
