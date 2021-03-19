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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Set;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * {@link Endpoint @Endpoint} that outputs metrics in a format that can be scraped by the
 * Prometheus server.
 *
 * @author Jon Schneider
 * @author Johnny Lim
 * @since 2.0.0
 */
@WebEndpoint(id = "prometheus")
public class PrometheusScrapeEndpoint {

	private final CollectorRegistry collectorRegistry;

	public PrometheusScrapeEndpoint(CollectorRegistry collectorRegistry) {
		this.collectorRegistry = collectorRegistry;
	}

	@ReadOperation(produces = { TextFormat.CONTENT_TYPE_004, TextFormat.CONTENT_TYPE_OPENMETRICS_100 })
	public WebEndpointResponse<String> scrape(ProducibleTextFormat producibleTextFormat,
			@Nullable Set<String> includedNames) {
		try {
			Writer writer = new StringWriter();
			Enumeration<MetricFamilySamples> samples = (includedNames != null)
					? this.collectorRegistry.filteredMetricFamilySamples(includedNames)
					: this.collectorRegistry.metricFamilySamples();
			MimeType contentType = producibleTextFormat.getMimeType();
			if (producibleTextFormat == ProducibleTextFormat.CONTENT_TYPE_004) {
				TextFormat.write004(writer, samples);
			}
			else if (producibleTextFormat == ProducibleTextFormat.CONTENT_TYPE_OPENMETRICS_100) {
				TextFormat.writeOpenMetrics100(writer, samples);
			}
			else {
				throw new RuntimeException("Unsupported text format '" + producibleTextFormat.getMimeType() + "'");
			}
			return new WebEndpointResponse<>(writer.toString(), contentType);
		}
		catch (IOException ex) {
			// This actually never happens since StringWriter::write() doesn't throw any
			// IOException
			throw new RuntimeException("Writing metrics failed", ex);
		}
	}

}
