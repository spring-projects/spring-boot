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

package org.springframework.boot.actuate.metrics.export.prometheus;

import java.io.IOException;
import java.io.OutputStream;

import io.prometheus.metrics.expositionformats.ExpositionFormats;
import io.prometheus.metrics.expositionformats.OpenMetricsTextFormatWriter;
import io.prometheus.metrics.expositionformats.PrometheusProtobufWriter;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;

import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * A {@link Producible} enum for supported Prometheus formats.
 *
 * @author Andy Wilkinson
 * @since 3.3.0
 */
public enum PrometheusOutputFormat implements Producible<PrometheusOutputFormat> {

	/**
	 * Prometheus text version 0.0.4.
	 */
	CONTENT_TYPE_004(PrometheusTextFormatWriter.CONTENT_TYPE) {

		@Override
		void write(OutputStream outputStream, MetricSnapshots snapshots) throws IOException {
			EXPOSITION_FORMATS.getPrometheusTextFormatWriter().write(outputStream, snapshots);
		}

		@Override
		public boolean isDefault() {
			return true;
		}

	},

	/**
	 * OpenMetrics text version 1.0.0.
	 */
	CONTENT_TYPE_OPENMETRICS_100(OpenMetricsTextFormatWriter.CONTENT_TYPE) {

		@Override
		void write(OutputStream outputStream, MetricSnapshots snapshots) throws IOException {
			EXPOSITION_FORMATS.getOpenMetricsTextFormatWriter().write(outputStream, snapshots);
		}

	},

	/**
	 * Prometheus metrics protobuf.
	 */
	CONTENT_TYPE_PROTOBUF(PrometheusProtobufWriter.CONTENT_TYPE) {

		@Override
		void write(OutputStream outputStream, MetricSnapshots snapshots) throws IOException {
			EXPOSITION_FORMATS.getPrometheusProtobufWriter().write(outputStream, snapshots);
		}

	};

	private static final ExpositionFormats EXPOSITION_FORMATS = ExpositionFormats.init();

	private final MimeType mimeType;

	PrometheusOutputFormat(String mimeType) {
		this.mimeType = MimeTypeUtils.parseMimeType(mimeType);
	}

	@Override
	public MimeType getProducedMimeType() {
		return this.mimeType;
	}

	abstract void write(OutputStream outputStream, MetricSnapshots snapshots) throws IOException;

}
