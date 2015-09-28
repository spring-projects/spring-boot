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

package org.springframework.boot.actuate.metrics.atsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * A {@link MetricWriter} for the ATSD database.
 *
 * @author Alexander Tokarev.
 */
public class AtsdMetricWriter implements MetricWriter, InitializingBean {
	private static final Log logger = LogFactory.getLog(AtsdMetricWriter.class);
	/**
	 * Default buffer size.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 64;
	/**
	 * Default ATSD URL.
	 */
	public static final String DEFAULT_URL = "http://localhost:8088/api/v1/command";
	private static final List<MediaType> ACCEPTABLE_MEDIA_TYPES = Collections.singletonList(MediaType.APPLICATION_JSON);
	private static final MediaType CONTENT_TYPE = MediaType.TEXT_PLAIN;
	private final ArrayList<AtsdData> buffer = new ArrayList<AtsdData>(DEFAULT_BUFFER_SIZE);
	private AtsdNamingStrategy namingStrategy = new DefaultAtsdNamingStrategy();
	private AtsdDataEncoder dataEncoder = new DefaultAtsdDataEncoder();
	private RestOperations restTemplate = new RestTemplate();
	private int bufferSize = DEFAULT_BUFFER_SIZE;
	private String url = DEFAULT_URL;
	private String username;
	private String password;
	private String basicAuthorizationHeader;

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setRestTemplate(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setNamingStrategy(AtsdNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public void setDataEncoder(AtsdDataEncoder dataEncoder) {
		this.dataEncoder = dataEncoder;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return this.url;
	}

	public AtsdDataEncoder getDataEncoder() {
		return this.dataEncoder;
	}

	@Override
	public void increment(Delta<?> delta) {
		throw new UnsupportedOperationException("Counters not supported via increment");
	}

	@Override
	public void reset(String metricName) {
		set(new Metric<Long>(metricName, 0L));
	}

	@Override
	public void set(Metric<?> value) {
		AtsdName name = this.namingStrategy.getName(value.getName());
		AtsdData data = new AtsdData(name, value.getValue(), value.getTimestamp().getTime());
		boolean needFlush;
		synchronized (this.buffer) {
			this.buffer.add(data);
			needFlush = this.buffer.size() >= this.bufferSize;
		}
		if (needFlush) {
			flush();
		}
	}

	public void flush() {
		AtsdData[] data;
		synchronized (this.buffer) {
			data = this.buffer.toArray(new AtsdData[this.buffer.size()]);
			this.buffer.clear();
		}
		if (data.length > 0) {
			String encoded = getDataEncoder().encode(data);
			HttpEntity<String> request = new HttpEntity<String>(encoded, createHeaders());
			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> response = this.restTemplate.postForEntity(getUrl(), request, Map.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				logger.warn("Cannot write metrics (discarded " + data.length + " values): " + response.getBody());
			}
		}
	}

	protected HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(ACCEPTABLE_MEDIA_TYPES);
		headers.setContentType(CONTENT_TYPE);
		headers.set(HttpHeaders.AUTHORIZATION, this.basicAuthorizationHeader);
		return headers;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.restTemplate, "RestTemplate is required");
		Assert.isTrue(this.bufferSize > 0, "Buffer size must be greater than 0");
		this.buffer.ensureCapacity(this.bufferSize);
		Assert.notNull(this.namingStrategy, "Naming strategy is required");
		Assert.notNull(this.dataEncoder, "Data encoder is required");
		Assert.hasText(this.url, "Url is required");
		Assert.hasText(this.username, "Username is required");
		Assert.hasText(this.password, "Password is required");
		byte[] token = Base64Utils.encode((this.username + ":" + this.password).getBytes());
		this.basicAuthorizationHeader = "Basic " + new String(token);
	}
}
