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

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OpenTsdbMetricWriter}.
 *
 * @author Dave Syer
 */
public class OpenTsdbMetricWriterTests {

	private OpenTsdbMetricWriter writer;
	private RestOperations restTemplate = Mockito.mock(RestOperations.class);

	@Before
	public void init() {
		this.writer = new OpenTsdbMetricWriter();
		this.writer.setRestTemplate(this.restTemplate);
	}

	@Test
	public void postSuccessfullyOnFlush() {
		this.writer.set(new Metric<Double>("foo", 2.4));
		given(this.restTemplate.postForEntity(anyString(), any(Object.class), anyMap()))
				.willReturn(emptyResponse());
		this.writer.flush();
		verify(this.restTemplate).postForEntity(anyString(), any(Object.class), anyMap());
	}

	@Test
	public void flushAutomaticlly() {
		given(this.restTemplate.postForEntity(anyString(), any(Object.class), anyMap()))
				.willReturn(emptyResponse());
		this.writer.setBufferSize(0);
		this.writer.set(new Metric<Double>("foo", 2.4));
		verify(this.restTemplate).postForEntity(anyString(), any(Object.class), anyMap());
	}

	@SuppressWarnings("rawtypes")
	private ResponseEntity<Map> emptyResponse() {
		return new ResponseEntity<Map>(Collections.emptyMap(), HttpStatus.OK);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Class<Map> anyMap() {
		return any(Class.class);
	}

}
