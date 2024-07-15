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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
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
		Assert.notNull(formatter, "Formatter must not be null");
		this.formatter = formatter;
	}

	@Override
	public String toSerializable(LogEvent event) {
		return this.formatter.format(event);
	}

	@PluginBuilderFactory
	static StructuredLogLayout.Builder newBuilder() {
		return new StructuredLogLayout.Builder();
	}

	static final class Builder implements org.apache.logging.log4j.core.util.Builder<StructuredLogLayout> {

		@PluginBuilderAttribute
		private String format;

		@PluginBuilderAttribute
		private String charset = StandardCharsets.UTF_8.name();

		@PluginBuilderAttribute
		private Long pid;

		@PluginBuilderAttribute
		private String serviceName;

		@PluginBuilderAttribute
		private String serviceVersion;

		@PluginBuilderAttribute
		private String serviceNodeName;

		@PluginBuilderAttribute
		private String serviceEnvironment;

		Builder setFormat(String format) {
			this.format = format;
			return this;
		}

		Builder setCharset(String charset) {
			this.charset = charset;
			return this;
		}

		Builder setPid(Long pid) {
			this.pid = pid;
			return this;
		}

		Builder setServiceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		Builder setServiceVersion(String serviceVersion) {
			this.serviceVersion = serviceVersion;
			return this;
		}

		Builder setServiceNodeName(String serviceNodeName) {
			this.serviceNodeName = serviceNodeName;
			return this;
		}

		Builder setServiceEnvironment(String serviceEnvironment) {
			this.serviceEnvironment = serviceEnvironment;
			return this;
		}

		@Override
		public StructuredLogLayout build() {
			ApplicationMetadata applicationMetadata = new ApplicationMetadata(this.pid, this.serviceName,
					this.serviceVersion, this.serviceEnvironment, this.serviceNodeName);
			Charset charset = Charset.forName(this.charset);
			StructuredLogFormatter<LogEvent> formatter = new StructuredLogFormatterFactory<>(LogEvent.class,
					applicationMetadata, null, this::addCommonFormatters)
				.get(this.format);
			return new StructuredLogLayout(charset, formatter);
		}

		private void addCommonFormatters(CommonFormatters<LogEvent> commonFormatters) {
			commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA,
					(instantiator) -> new ElasticCommonSchemaStructuredLogFormatter(
							instantiator.getArg(ApplicationMetadata.class)));
			commonFormatters.add(CommonStructuredLogFormat.LOGSTASH,
					(instantiator) -> new LogstashStructuredLogFormatter());
		}

	}

}
