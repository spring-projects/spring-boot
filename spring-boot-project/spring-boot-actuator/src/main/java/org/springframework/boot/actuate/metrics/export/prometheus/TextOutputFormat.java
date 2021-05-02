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

package org.springframework.boot.actuate.metrics.export.prometheus;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.exporter.common.TextFormat;

import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * A {@link Producible} enum for supported Prometheus {@link TextFormat}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public enum TextOutputFormat implements Producible<TextOutputFormat> {

	/**
	 * OpenMetrics text version 1.0.0.
	 */
	CONTENT_TYPE_OPENMETRICS_100(TextFormat.CONTENT_TYPE_OPENMETRICS_100) {

		@Override
		void write(Writer writer, Enumeration<MetricFamilySamples> samples) throws IOException {
			TextFormat.writeOpenMetrics100(writer, samples);
		}

	},

	/**
	 * Prometheus text version 0.0.4.
	 */
	CONTENT_TYPE_004(TextFormat.CONTENT_TYPE_004) {

		@Override
		void write(Writer writer, Enumeration<MetricFamilySamples> samples) throws IOException {
			TextFormat.write004(writer, samples);
		}

	};

	private final MimeType mimeType;

	TextOutputFormat(String mimeType) {
		this.mimeType = MimeTypeUtils.parseMimeType(mimeType);
	}

	@Override
	public MimeType getProducedMimeType() {
		return this.mimeType;
	}

	abstract void write(Writer writer, Enumeration<MetricFamilySamples> samples) throws IOException;

}
