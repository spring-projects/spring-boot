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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormattingConversionService;

/**
 * Factory to create {@link ConversionService ConversionServices} for test
 * {@link Parameters}.
 *
 * @author Phillip Webb
 */
public class ConversionServiceParameters implements Iterable<Object[]> {

	private final List<Object[]> parameters;

	public ConversionServiceParameters(Formatter<?> formatter) {
		this((Consumer<FormattingConversionService>) (conversionService) -> conversionService.addFormatter(formatter));
	}

	public ConversionServiceParameters(ConverterFactory<?, ?> converterFactory) {
		this((Consumer<FormattingConversionService>) (conversionService) -> conversionService
				.addConverterFactory(converterFactory));
	}

	public ConversionServiceParameters(GenericConverter converter) {
		this((Consumer<FormattingConversionService>) (conversionService) -> conversionService.addConverter(converter));
	}

	public ConversionServiceParameters(Consumer<FormattingConversionService> initializer) {
		FormattingConversionService withoutDefaults = new FormattingConversionService();
		initializer.accept(withoutDefaults);
		List<Object[]> parameters = new ArrayList<>();
		parameters.add(new Object[] { "without defaults conversion service", withoutDefaults });
		parameters.add(new Object[] { "application conversion service", new ApplicationConversionService() });
		this.parameters = Collections.unmodifiableList(parameters);
	}

	@Override
	public Iterator<Object[]> iterator() {
		return this.parameters.iterator();
	}

}
