/*
 * Copyright 2012-present the original author or authors.
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.WritableJson;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.ContextPairs.Joiner;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatProperties;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Log4j2 {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#GRAYLOG_EXTENDED_LOG_FORMAT}. Supports GELF version
 * 1.1.
 *
 * @author Samuel Lissner
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class GraylogExtendedLogFormatStructuredLogFormatter extends JsonWriterStructuredLogFormatter<LogEvent> {

	private static final Log logger = LogFactory.getLog(GraylogExtendedLogFormatStructuredLogFormatter.class);

	/**
	 * Allowed characters in field names are any word character (letter, number,
	 * underscore), dashes and dots.
	 */
	private static final Pattern FIELD_NAME_VALID_PATTERN = Pattern.compile("^[\\w.\\-]*$");

	/**
	 * Libraries SHOULD not allow to send id as additional field ("_id"). Graylog server
	 * nodes omit this field automatically.
	 */
	private static final Set<String> ADDITIONAL_FIELD_ILLEGAL_KEYS = Set.of("id", "_id");

	GraylogExtendedLogFormatStructuredLogFormatter(Environment environment,
			@Nullable StackTracePrinter stackTracePrinter, ContextPairs contextPairs,
			@Nullable StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(environment, stackTracePrinter, contextPairs, members), customizer);
	}

	private static void jsonMembers(Environment environment, @Nullable StackTracePrinter stackTracePrinter,
			ContextPairs contextPairs, JsonWriter.Members<LogEvent> members) {
		Extractor extractor = new Extractor(stackTracePrinter);
		members.add("version", "1.1");
		members.add("short_message", LogEvent::getMessage)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::getMessageText);
		members.add("timestamp", LogEvent::getInstant)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::formatTimeStamp);
		members.add("level", GraylogExtendedLogFormatStructuredLogFormatter::convertLevel);
		members.add("_level_name", LogEvent::getLevel).as(Level::name);
		members.add("_process_pid", environment.getProperty("spring.application.pid", Long.class)).whenNotNull();
		members.add("_process_thread_name", LogEvent::getThreadName);
		GraylogExtendedLogFormatProperties.get(environment).jsonMembers(members);
		members.add("_log_logger", LogEvent::getLoggerName);
		Predicate<@Nullable ReadOnlyStringMap> mapIsEmpty = (map) -> map == null || map.isEmpty();
		members.from(LogEvent::getContextData)
			.whenNot(mapIsEmpty)
			.usingPairs(contextPairs.flat(additionalFieldJoiner(),
					GraylogExtendedLogFormatStructuredLogFormatter::addContextDataPairs));
		Function<@Nullable LogEvent, @Nullable Object> getThrown = (event) -> (event != null) ? event.getThrown()
				: null;
		members.add()
			.whenNotNull(getThrown)
			.usingMembers((thrownMembers) -> throwableMembers(thrownMembers, extractor));
	}

	private static String getMessageText(Message message) {
		// Always return text as a blank message will lead to a error as of Graylog v6
		String formattedMessage = message.getFormattedMessage();
		return (!StringUtils.hasText(formattedMessage)) ? "(blank)" : formattedMessage;
	}

	/**
	 * GELF requires "seconds since UNIX epoch with optional <b>decimal places for
	 * milliseconds</b>". To comply with this requirement, we format a POSIX timestamp
	 * with millisecond precision as e.g. "1725459730385" -> "1725459730.385"
	 * @param timeStamp the timestamp of the log message.
	 * @return the timestamp formatted as string with millisecond precision
	 */
	private static WritableJson formatTimeStamp(Instant timeStamp) {
		return (out) -> out.append(new BigDecimal(timeStamp.getEpochMillisecond()).movePointLeft(3).toPlainString());
	}

	/**
	 * Converts the log4j2 event level to the Syslog event level code.
	 * @param event the log event
	 * @return an integer representing the syslog log level code
	 * @see Severity class from Log4j2 which contains the conversion logic
	 */
	private static int convertLevel(LogEvent event) {
		return Severity.getSeverity(event.getLevel()).getCode();
	}

	private static void throwableMembers(Members<LogEvent> members, Extractor extractor) {
		members.add("full_message", extractor::messageAndStackTrace);
		members.add("_error_type", LogEvent::getThrown).whenNotNull().as(ObjectUtils::nullSafeClassName);
		members.add("_error_stack_trace", extractor::stackTrace);
		members.add("_error_message", (event) -> event.getThrown().getMessage());
	}

	private static void addContextDataPairs(ContextPairs.Pairs<ReadOnlyStringMap> contextPairs) {
		contextPairs.add((contextData, pairs) -> contextData.forEach(pairs::accept));
	}

	private static Joiner additionalFieldJoiner() {
		return (prefix, name) -> {
			name = prefix + name;
			if (!FIELD_NAME_VALID_PATTERN.matcher(name).matches()) {
				logger.warn(LogMessage.format("'%s' is not a valid field name according to GELF standard", name));
				return null;
			}
			if (ADDITIONAL_FIELD_ILLEGAL_KEYS.contains(name)) {
				logger.warn(LogMessage.format("'%s' is an illegal field name according to GELF standard", name));
				return null;
			}
			return (!name.startsWith("_")) ? "_" + name : name;
		};
	}

}
