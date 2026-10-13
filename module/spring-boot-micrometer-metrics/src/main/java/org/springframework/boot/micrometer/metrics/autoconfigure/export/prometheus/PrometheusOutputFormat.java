/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus;

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
 * @since 4.0.0
 */
public enum PrometheusOutputFormat implements Producible<PrometheusOutputFormat> {

	/**
	 * Prometheus text version 0.0.4.
	 */
	CONTENT_TYPE_004(PrometheusTextFormatWriter.CONTENT_TYPE) {

		@Override
		void write(ExpositionFormats expositionFormats, OutputStream outputStream, MetricSnapshots snapshots)
				throws IOException {
			expositionFormats.getPrometheusTextFormatWriter().write(outputStream, snapshots);
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
		void write(ExpositionFormats expositionFormats, OutputStream outputStream, MetricSnapshots snapshots)
				throws IOException {
			expositionFormats.getOpenMetricsTextFormatWriter().write(outputStream, snapshots);
		}

	},

	/**
	 * Prometheus metrics protobuf.
	 */
	CONTENT_TYPE_PROTOBUF(PrometheusProtobufWriter.CONTENT_TYPE) {

		@Override
		void write(ExpositionFormats expositionFormats, OutputStream outputStream, MetricSnapshots snapshots)
				throws IOException {
			expositionFormats.getPrometheusProtobufWriter().write(outputStream, snapshots);
		}

	};

	private final MimeType mimeType;

	PrometheusOutputFormat(String mimeType) {
		this.mimeType = MimeTypeUtils.parseMimeType(mimeType);
	}

	@Override
	public MimeType getProducedMimeType() {
		return this.mimeType;
	}

	abstract void write(ExpositionFormats expositionFormats, OutputStream outputStream, MetricSnapshots snapshots)
			throws IOException;

}
