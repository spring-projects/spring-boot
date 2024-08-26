/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatService;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#GRAYLOG_EXTENDED_LOG_FORMAT}. Supports GELF version
 * 1.1.
 *
 * @author Samuel Lissner
 */
class GraylogExtendedLogFormatStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

	/**
	 * Allowed characters in field names are any word character (letter, number,
	 * underscore), dashes and dots.
	 */
	private static final Pattern FIELD_NAME_VALID_PATTERN = Pattern.compile("^[\\w\\.\\-]*$");

	/**
	 * Every field been sent and prefixed with an underscore "_" will be treated as an
	 * additional field.
	 */
	private static final String ADDITIONAL_FIELD_PREFIX = "_";

	/**
	 * Libraries SHOULD not allow to send id as additional field ("_id"). Graylog server
	 * nodes omit this field automatically.
	 */
	private static final Set<String> ADDITIONAL_FIELD_ILLEGAL_KEYS = Set.of("_id");

	/**
	 * Default format to be used for the `full_message` property when there is a throwable
	 * present in the log event.
	 */
	private static final String DEFAULT_FULL_MESSAGE_WITH_THROWABLE_FORMAT = "%s%n%n%s";

	private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor
		.of((pair) -> makeAdditionalFieldName(pair.key), (pair) -> pair.value);

	GraylogExtendedLogFormatStructuredLogFormatter(Environment environment,
			ThrowableProxyConverter throwableProxyConverter) {
		super((members) -> jsonMembers(environment, throwableProxyConverter, members));
	}

	private static void jsonMembers(Environment environment, ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		members.add("version", "1.1");

		// note: a blank message will lead to a Graylog error as of Graylog v6.0.x. We are
		// ignoring this here.
		members.add("short_message", ILoggingEvent::getFormattedMessage);

		members.add("timestamp", ILoggingEvent::getTimeStamp)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::formatTimeStamp);
		members.add("level", LevelToSyslogSeverity::convert);
		members.add("_level_name", ILoggingEvent::getLevel);

		members.add("_process_pid", environment.getProperty("spring.application.pid", Long.class))
			.when(Objects::nonNull);
		members.add("_process_thread_name", ILoggingEvent::getThreadName);

		GraylogExtendedLogFormatService.get(environment).jsonMembers(members);

		members.add("_log_logger", ILoggingEvent::getLoggerName);

		members.addMapEntries(mapMDCProperties(ILoggingEvent::getMDCPropertyMap));

		members.from(ILoggingEvent::getKeyValuePairs)
			.whenNotEmpty()
			.usingExtractedPairs(Iterable::forEach, keyValuePairExtractor);

		members.add().whenNotNull(ILoggingEvent::getThrowableProxy).usingMembers((throwableMembers) -> {
			throwableMembers.add("full_message",
					(event) -> formatFullMessageWithThrowable(throwableProxyConverter, event));
			throwableMembers.add("_error_type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
			throwableMembers.add("_error_stack_trace", throwableProxyConverter::convert);
			throwableMembers.add("_error_message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
		});
	}

	/**
	 * GELF requires "seconds since UNIX epoch with optional <b>decimal places for
	 * milliseconds</b>". To comply with this requirement, we format a POSIX timestamp
	 * with millisecond precision as e.g. "1725459730385" -> "1725459730.385"
	 * @param timeStamp the timestamp of the log message
	 * @return the timestamp formatted as string with millisecond precision
	 */
	private static double formatTimeStamp(final long timeStamp) {
		return new BigDecimal(timeStamp).movePointLeft(3).doubleValue();
	}

	private static String formatFullMessageWithThrowable(final ThrowableProxyConverter throwableProxyConverter,
			ILoggingEvent event) {
		return String.format(DEFAULT_FULL_MESSAGE_WITH_THROWABLE_FORMAT, event.getFormattedMessage(),
				throwableProxyConverter.convert(event));
	}

	private static Function<ILoggingEvent, Map<String, String>> mapMDCProperties(
			Function<ILoggingEvent, Map<String, String>> MDCPropertyMapGetter) {
		return MDCPropertyMapGetter.andThen((mdc) -> mdc.entrySet()
			.stream()
			.collect(Collectors.toMap((entry) -> makeAdditionalFieldName(entry.getKey()), Map.Entry::getValue)));
	}

	private static String makeAdditionalFieldName(String fieldName) {
		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.isTrue(FIELD_NAME_VALID_PATTERN.matcher(fieldName).matches(),
				() -> String.format("fieldName must be a valid according to GELF standard. [fieldName=%s]", fieldName));
		Assert.isTrue(!ADDITIONAL_FIELD_ILLEGAL_KEYS.contains(fieldName), () -> String.format(
				"fieldName must not be an illegal additional field key according to GELF standard. [fieldName=%s]",
				fieldName));

		if (fieldName.startsWith(ADDITIONAL_FIELD_PREFIX)) {
			// No need to prepend the `ADDITIONAL_FIELD_PREFIX` in case the caller already
			// has prepended the prefix.
			return fieldName;
		}

		return ADDITIONAL_FIELD_PREFIX + fieldName;
	}

}
