/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.format;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.format.support.FormattingConversionService} dedicated to web
 * applications for formatting and converting values to/from the web.
 * <p>
 * This service replaces the default implementations provided by
 * {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}
 * and {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class WebConversionService extends DefaultFormattingConversionService {

	private static final boolean JSR_354_PRESENT = ClassUtils.isPresent("javax.money.MonetaryAmount",
			WebConversionService.class.getClassLoader());

	/**
	 * Create a new WebConversionService that configures formatters with the provided
	 * date, time, and date-time formats, or registers the default if no custom format is
	 * provided.
	 * @param dateTimeFormatters the formatters to use for date, time, and date-time
	 * formatting
	 * @since 2.3.0
	 */
	public WebConversionService(DateTimeFormatters dateTimeFormatters) {
		super(false);
		if (dateTimeFormatters.isCustomized()) {
			addFormatters(dateTimeFormatters);
		}
		else {
			addDefaultFormatters(this);
		}
	}

	private void addFormatters(DateTimeFormatters dateTimeFormatters) {
		addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
		if (JSR_354_PRESENT) {
			addFormatter(new CurrencyUnitFormatter());
			addFormatter(new MonetaryAmountFormatter());
			addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
		}
		registerJsr310(dateTimeFormatters);
		registerJavaDate(dateTimeFormatters);
	}

	private void registerJsr310(DateTimeFormatters dateTimeFormatters) {
		DateTimeFormatterRegistrar dateTime = new DateTimeFormatterRegistrar();
		configure(dateTimeFormatters::getDateFormatter, dateTime::setDateFormatter);
		configure(dateTimeFormatters::getTimeFormatter, dateTime::setTimeFormatter);
		configure(dateTimeFormatters::getDateTimeFormatter, dateTime::setDateTimeFormatter);
		dateTime.registerFormatters(this);
	}

	private void configure(Supplier<DateTimeFormatter> supplier, Consumer<DateTimeFormatter> consumer) {
		DateTimeFormatter formatter = supplier.get();
		if (formatter != null) {
			consumer.accept(formatter);
		}
	}

	private void registerJavaDate(DateTimeFormatters dateTimeFormatters) {
		DateFormatterRegistrar dateFormatterRegistrar = new DateFormatterRegistrar();
		String datePattern = dateTimeFormatters.getDatePattern();
		if (datePattern != null) {
			DateFormatter dateFormatter = new DateFormatter(datePattern);
			dateFormatterRegistrar.setFormatter(dateFormatter);
		}
		dateFormatterRegistrar.registerFormatters(this);
	}

}
