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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * A {@link Formatter} for {@link OffsetDateTime} that uses
 * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME ISO offset formatting}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class IsoOffsetFormatter implements Formatter<OffsetDateTime> {

	/**
     * Formats the given OffsetDateTime object into a string representation using the ISO_OFFSET_DATE_TIME format.
     * 
     * @param object the OffsetDateTime object to be formatted
     * @param locale the locale to be used for formatting (not used in this method)
     * @return the formatted string representation of the OffsetDateTime object
     */
    @Override
	public String print(OffsetDateTime object, Locale locale) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(object);
	}

	/**
     * Parses the given text into an OffsetDateTime object using the ISO_OFFSET_DATE_TIME format.
     *
     * @param text   the text to be parsed
     * @param locale the locale to be used for parsing (ignored in this implementation)
     * @return the parsed OffsetDateTime object
     * @throws ParseException if the text cannot be parsed into an OffsetDateTime object
     */
    @Override
	public OffsetDateTime parse(String text, Locale locale) throws ParseException {
		return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

}
