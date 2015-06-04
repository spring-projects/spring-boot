/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.opentsdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * A {@link MetricWriter} for the Open TSDB database (version 2.0), writing metrics to the
 * HTTP endpoint provided by the server. Data are buffered according to the
 * {@link #setBufferSize(int) bufferSize} property, and only flushed automatically when
 * the buffer size is reached. Users should either manually {@link #flush()} after writing
 * a batch of data if that makes sense, or consider adding a {@link Scheduled
 * <code>@Scheduled</code>} task to flush periodically.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class OpenTsdbMetricWriter implements MetricWriter {

	private static final Log logger = LogFactory.getLog(OpenTsdbMetricWriter.class);

	private RestOperations restTemplate = new RestTemplate();

	/**
	 * URL for POSTing data. Defaults to http://localhost:4242/api/put.
	 */
	private String url = "http://localhost:4242/api/put";

	/**
	 * Buffer size to fill before posting data to server.
	 */
	private int bufferSize = 64;

	/**
	 * The media type to use to serialize and accept responses from the server. Defaults
	 * to "application/json".
	 */
	private MediaType mediaType = MediaType.APPLICATION_JSON;

	private List<OpenTsdbData> buffer = new ArrayList<OpenTsdbData>(this.bufferSize);

	private OpenTsdbNamingStrategy namingStrategy = new DefaultOpenTsdbNamingStrategy();

	public RestOperations getRestTemplate() {
		return this.restTemplate;
	}

	public void setRestTemplate(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	public void setNamingStrategy(OpenTsdbNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	@Override
	public void increment(Delta<?> delta) {
		throw new UnsupportedOperationException("Counters not supported via increment");
	}

	@Override
	public void set(Metric<?> value) {
		OpenTsdbData data = new OpenTsdbData(
				this.namingStrategy.getName(value.getName()), value.getValue(), value
						.getTimestamp().getTime());
		this.buffer.add(data);
		if (this.buffer.size() >= this.bufferSize) {
			flush();
		}
	}

	/**
	 * Flush the buffer without waiting for it to fill any further.
	 */
	public void flush() {
		if (this.buffer.isEmpty()) {
			return;
		}
		List<OpenTsdbData> temp = new ArrayList<OpenTsdbData>();
		synchronized (this.buffer) {
			temp.addAll(this.buffer);
			this.buffer.clear();
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(this.mediaType));
		headers.setContentType(this.mediaType);
		HttpEntity<List<OpenTsdbData>> request = new HttpEntity<List<OpenTsdbData>>(temp,
				headers);
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = this.restTemplate.postForEntity(this.url, request,
				Map.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			logger.warn("Cannot write metrics (discarded " + temp.size() + " values): "
					+ response.getBody());
		}
	}

	@Override
	public void reset(String metricName) {
		set(new Metric<Long>(metricName, 0L));
	}

}
