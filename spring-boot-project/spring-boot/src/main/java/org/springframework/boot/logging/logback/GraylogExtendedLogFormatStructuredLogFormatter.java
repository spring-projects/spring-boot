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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.WritableJson;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatProperties;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#GRAYLOG_EXTENDED_LOG_FORMAT}. Supports GELF version
 * 1.1.
 *
 * @author Samuel Lissner
 * @author Moritz Halbritter
 */
class GraylogExtendedLogFormatStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

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
			ThrowableProxyConverter throwableProxyConverter, StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(environment, throwableProxyConverter, members), customizer);
	}

	private static void jsonMembers(Environment environment, ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		members.add("version", "1.1");
		members.add("short_message", ILoggingEvent::getFormattedMessage)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::getMessageText);
		members.add("timestamp", ILoggingEvent::getTimeStamp)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::formatTimeStamp);
		members.add("level", LevelToSyslogSeverity::convert);
		members.add("_level_name", ILoggingEvent::getLevel);
		members.add("_process_pid", environment.getProperty("spring.application.pid", Long.class))
			.when(Objects::nonNull);
		members.add("_process_thread_name", ILoggingEvent::getThreadName);
		GraylogExtendedLogFormatProperties.get(environment).jsonMembers(members);
		members.add("_log_logger", ILoggingEvent::getLoggerName);
		members.from(ILoggingEvent::getMDCPropertyMap)
			.when((mdc) -> !CollectionUtils.isEmpty(mdc))
			.usingPairs((mdc, pairs) -> mdc.forEach((key, value) -> createAdditionalField(key, value, pairs)));
		members.from(ILoggingEvent::getKeyValuePairs)
			.when((keyValuePairs) -> !CollectionUtils.isEmpty(keyValuePairs))
			.usingPairs(GraylogExtendedLogFormatStructuredLogFormatter::createAdditionalField);
		members.add()
			.whenNotNull(ILoggingEvent::getThrowableProxy)
			.usingMembers((throwableMembers) -> throwableMembers(throwableMembers, throwableProxyConverter));
	}

	private static String getMessageText(String formattedMessage) {
		// Always return text as a blank message will lead to a error as of Graylog v6
		return (!StringUtils.hasText(formattedMessage)) ? "(blank)" : formattedMessage;
	}

	/**
	 * GELF requires "seconds since UNIX epoch with optional <b>decimal places for
	 * milliseconds</b>". To comply with this requirement, we format a POSIX timestamp
	 * with millisecond precision as e.g. "1725459730385" -> "1725459730.385"
	 * @param timeStamp the timestamp of the log message
	 * @return the timestamp formatted as string with millisecond precision
	 */
	private static WritableJson formatTimeStamp(long timeStamp) {
		return (out) -> out.append(new BigDecimal(timeStamp).movePointLeft(3).toPlainString());
	}

	private static void throwableMembers(Members<ILoggingEvent> members,
			ThrowableProxyConverter throwableProxyConverter) {
		members.add("full_message", (event) -> formatFullMessageWithThrowable(throwableProxyConverter, event));
		members.add("_error_type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
		members.add("_error_stack_trace", throwableProxyConverter::convert);
		members.add("_error_message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
	}

	private static String formatFullMessageWithThrowable(ThrowableProxyConverter throwableProxyConverter,
			ILoggingEvent event) {
		return event.getFormattedMessage() + "\n\n" + throwableProxyConverter.convert(event);
	}

	private static void createAdditionalField(List<KeyValuePair> keyValuePairs, BiConsumer<Object, Object> pairs) {
		keyValuePairs.forEach((keyValuePair) -> createAdditionalField(keyValuePair.key, keyValuePair.value, pairs));
	}

	private static void createAdditionalField(String name, Object value, BiConsumer<Object, Object> pairs) {
		Assert.notNull(name, "'name' must not be null");
		if (!FIELD_NAME_VALID_PATTERN.matcher(name).matches()) {
			logger.warn(LogMessage.format("'%s' is not a valid field name according to GELF standard", name));
			return;
		}
		if (ADDITIONAL_FIELD_ILLEGAL_KEYS.contains(name)) {
			logger.warn(LogMessage.format("'%s' is an illegal field name according to GELF standard", name));
			return;
		}
		pairs.accept(asAdditionalFieldName(name), value);
	}

	private static Object asAdditionalFieldName(String name) {
		return (!name.startsWith("_")) ? "_" + name : name;
	}

}
