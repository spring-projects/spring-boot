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

package org.springframework.boot.logging.log4j2;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatService;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Log4j2 {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#GRAYLOG_EXTENDED_LOG_FORMAT}. Supports GELF version
 * 1.1.
 *
 * @author Samuel Lissner
 */
class GraylogExtendedLogFormatStructuredLogFormatter extends JsonWriterStructuredLogFormatter<LogEvent> {

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

	GraylogExtendedLogFormatStructuredLogFormatter(Environment environment) {
		super((members) -> jsonMembers(environment, members));
	}

	private static void jsonMembers(Environment environment, JsonWriter.Members<LogEvent> members) {
		members.add("version", "1.1");

		// note: a blank message will lead to a Graylog error as of Graylog v6.0.x. We are
		// ignoring this here.
		members.add("short_message", LogEvent::getMessage).as(Message::getFormattedMessage);

		members.add("timestamp", LogEvent::getInstant)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::formatTimeStamp);
		members.add("level", GraylogExtendedLogFormatStructuredLogFormatter::convertLevel);
		members.add("_level_name", LogEvent::getLevel).as(Level::name);

		members.add("_process_pid", environment.getProperty("spring.application.pid", Long.class))
			.when(Objects::nonNull);
		members.add("_process_thread_name", LogEvent::getThreadName);

		GraylogExtendedLogFormatService.get(environment).jsonMembers(members);

		members.add("_log_logger", LogEvent::getLoggerName);

		members.from(LogEvent::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.usingPairs((contextData, pairs) -> contextData
				.forEach((key, value) -> pairs.accept(makeAdditionalFieldName(key), value)));

		members.add().whenNotNull(LogEvent::getThrownProxy).usingMembers((eventMembers) -> {
			final Function<LogEvent, ThrowableProxy> throwableProxyGetter = LogEvent::getThrownProxy;

			eventMembers.add("full_message",
					GraylogExtendedLogFormatStructuredLogFormatter::formatFullMessageWithThrowable);
			eventMembers.add("_error_type", throwableProxyGetter.andThen(ThrowableProxy::getThrowable))
				.whenNotNull()
				.as(ObjectUtils::nullSafeClassName);
			eventMembers.add("_error_stack_trace",
					throwableProxyGetter.andThen(ThrowableProxy::getExtendedStackTraceAsString));
			eventMembers.add("_error_message", throwableProxyGetter.andThen(ThrowableProxy::getMessage));
		});
	}

	/**
	 * GELF requires "seconds since UNIX epoch with optional <b>decimal places for
	 * milliseconds</b>". To comply with this requirement, we format a POSIX timestamp
	 * with millisecond precision as e.g. "1725459730385" -> "1725459730.385"
	 * @param timeStamp the timestamp of the log message. Note it is not the standard Java
	 * `Instant` type but {@link org.apache.logging.log4j.core.time}
	 * @return the timestamp formatted as string with millisecond precision
	 */
	private static double formatTimeStamp(final Instant timeStamp) {
		return new BigDecimal(timeStamp.getEpochMillisecond()).movePointLeft(3).doubleValue();
	}

	/**
	 * Converts the log4j2 event level to the Syslog event level code.
	 * @param event the log event
	 * @return an integer representing the syslog log level code
	 * @see Severity class from Log4j2 which contains the conversion logic
	 */
	private static int convertLevel(final LogEvent event) {
		return Severity.getSeverity(event.getLevel()).getCode();
	}

	private static String formatFullMessageWithThrowable(final LogEvent event) {
		return String.format(DEFAULT_FULL_MESSAGE_WITH_THROWABLE_FORMAT, event.getMessage().getFormattedMessage(),
				event.getThrownProxy().getExtendedStackTraceAsString());
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
