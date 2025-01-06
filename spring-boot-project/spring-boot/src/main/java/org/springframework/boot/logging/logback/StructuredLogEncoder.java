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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;

import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.boot.util.Instantiator;
import org.springframework.boot.util.Instantiator.AvailableParameters;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link Encoder Logback encoder} for structured logging.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.4.0
 * @see StructuredLogFormatter
 */
public class StructuredLogEncoder extends EncoderBase<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

	private String format;

	private StructuredLogFormatter<ILoggingEvent> formatter;

	private Charset charset = StandardCharsets.UTF_8;

	public void setFormat(String format) {
		this.format = format;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public void start() {
		Assert.state(this.format != null, "Format has not been set");
		this.formatter = createFormatter(this.format);
		super.start();
		this.throwableProxyConverter.start();
	}

	private StructuredLogFormatter<ILoggingEvent> createFormatter(String format) {
		Environment environment = (Environment) getContext().getObject(Environment.class.getName());
		Assert.state(environment != null, "Unable to find Spring Environment in logger context");
		return new StructuredLogFormatterFactory<>(ILoggingEvent.class, environment, this::addAvailableParameters,
				this::addCommonFormatters)
			.get(format);
	}

	private void addAvailableParameters(AvailableParameters availableParameters) {
		availableParameters.add(ThrowableProxyConverter.class, this.throwableProxyConverter);
	}

	private void addCommonFormatters(CommonFormatters<ILoggingEvent> commonFormatters) {
		commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA, this::createEcsFormatter);
		commonFormatters.add(CommonStructuredLogFormat.GRAYLOG_EXTENDED_LOG_FORMAT, this::createGraylogFormatter);
		commonFormatters.add(CommonStructuredLogFormat.LOGSTASH, this::createLogstashFormatter);
	}

	private StructuredLogFormatter<ILoggingEvent> createEcsFormatter(Instantiator<?> instantiator) {
		Environment environment = instantiator.getArg(Environment.class);
		ThrowableProxyConverter throwableProxyConverter = instantiator.getArg(ThrowableProxyConverter.class);
		StructuredLoggingJsonMembersCustomizer<?> jsonMembersCustomizer = instantiator
			.getArg(StructuredLoggingJsonMembersCustomizer.class);
		return new ElasticCommonSchemaStructuredLogFormatter(environment, throwableProxyConverter,
				jsonMembersCustomizer);
	}

	private StructuredLogFormatter<ILoggingEvent> createGraylogFormatter(Instantiator<?> instantiator) {
		Environment environment = instantiator.getArg(Environment.class);
		ThrowableProxyConverter throwableProxyConverter = instantiator.getArg(ThrowableProxyConverter.class);
		StructuredLoggingJsonMembersCustomizer<?> jsonMembersCustomizer = instantiator
			.getArg(StructuredLoggingJsonMembersCustomizer.class);
		return new GraylogExtendedLogFormatStructuredLogFormatter(environment, throwableProxyConverter,
				jsonMembersCustomizer);
	}

	private StructuredLogFormatter<ILoggingEvent> createLogstashFormatter(Instantiator<?> instantiator) {
		ThrowableProxyConverter throwableProxyConverter = instantiator.getArg(ThrowableProxyConverter.class);
		StructuredLoggingJsonMembersCustomizer<?> jsonMembersCustomizer = instantiator
			.getArg(StructuredLoggingJsonMembersCustomizer.class);
		return new LogstashStructuredLogFormatter(throwableProxyConverter, jsonMembersCustomizer);
	}

	@Override
	public void stop() {
		this.throwableProxyConverter.stop();
		super.stop();
	}

	@Override
	public byte[] headerBytes() {
		return null;
	}

	@Override
	public byte[] encode(ILoggingEvent event) {
		return this.formatter.formatAsBytes(event, (this.charset != null) ? this.charset : StandardCharsets.UTF_8);
	}

	@Override
	public byte[] footerBytes() {
		return null;
	}

}
