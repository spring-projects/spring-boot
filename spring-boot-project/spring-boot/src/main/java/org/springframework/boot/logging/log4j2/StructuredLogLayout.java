/*
 * Copyright 2012-2025 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginLoggerContext;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.boot.util.Instantiator;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link Layout Log4j2 Layout} for structured logging.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @see StructuredLogFormatter
 */
@Plugin(name = "StructuredLogLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
final class StructuredLogLayout extends AbstractStringLayout {

	private final StructuredLogFormatter<LogEvent> formatter;

	private StructuredLogLayout(Charset charset, StructuredLogFormatter<LogEvent> formatter) {
		super(charset);
		Assert.notNull(formatter, "'formatter' must not be null");
		this.formatter = formatter;
	}

	@Override
	public String toSerializable(LogEvent event) {
		return this.formatter.format(event);
	}

	@Override
	public byte[] toByteArray(LogEvent event) {
		return this.formatter.formatAsBytes(event, (getCharset() != null) ? getCharset() : StandardCharsets.UTF_8);
	}

	@PluginBuilderFactory
	static StructuredLogLayout.Builder newBuilder() {
		return new StructuredLogLayout.Builder();
	}

	static final class Builder implements org.apache.logging.log4j.core.util.Builder<StructuredLogLayout> {

		@PluginLoggerContext
		private LoggerContext loggerContext;

		@PluginBuilderAttribute
		private String format;

		@PluginBuilderAttribute
		private String charset = StandardCharsets.UTF_8.name();

		Builder setFormat(String format) {
			this.format = format;
			return this;
		}

		Builder setCharset(String charset) {
			this.charset = charset;
			return this;
		}

		@Override
		public StructuredLogLayout build() {
			Charset charset = Charset.forName(this.charset);
			Environment environment = Log4J2LoggingSystem.getEnvironment(this.loggerContext);
			Assert.state(environment != null, "Unable to find Spring Environment in logger context");
			StructuredLogFormatter<LogEvent> formatter = new StructuredLogFormatterFactory<>(LogEvent.class,
					environment, null, this::addCommonFormatters)
				.get(this.format);
			return new StructuredLogLayout(charset, formatter);
		}

		private void addCommonFormatters(CommonFormatters<LogEvent> commonFormatters) {
			commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA, this::createEcsFormatter);
			commonFormatters.add(CommonStructuredLogFormat.GRAYLOG_EXTENDED_LOG_FORMAT, this::createGraylogFormatter);
			commonFormatters.add(CommonStructuredLogFormat.LOGSTASH, this::createLogstashFormatter);
		}

		private ElasticCommonSchemaStructuredLogFormatter createEcsFormatter(Instantiator<?> instantiator) {
			Environment environment = instantiator.getArg(Environment.class);
			StructuredLoggingJsonMembersCustomizer<?> jsonMembersCustomizer = instantiator
				.getArg(StructuredLoggingJsonMembersCustomizer.class);
			return new ElasticCommonSchemaStructuredLogFormatter(environment, jsonMembersCustomizer);
		}

		private GraylogExtendedLogFormatStructuredLogFormatter createGraylogFormatter(Instantiator<?> instantiator) {
			Environment environment = instantiator.getArg(Environment.class);
			StructuredLoggingJsonMembersCustomizer<?> jsonMembersCustomizer = instantiator
				.getArg(StructuredLoggingJsonMembersCustomizer.class);
			return new GraylogExtendedLogFormatStructuredLogFormatter(environment, jsonMembersCustomizer);
		}

		private LogstashStructuredLogFormatter createLogstashFormatter(Instantiator<?> instantiator) {
			StructuredLoggingJsonMembersCustomizer<?> jsonMembersCustomizer = instantiator
				.getArg(StructuredLoggingJsonMembersCustomizer.class);
			return new LogstashStructuredLogFormatter(jsonMembersCustomizer);
		}

	}

}
