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

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;


/**
 * Tests for {@link AtsdMetricWriter}.
 *
 * @author Alexander Tokarev.
 */
public class AtsdMetricWriterTests {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private AtsdMetricWriter atsdMetricWriter;
	private MockRestServiceServer mockServer;

	@Before
	public void setUp() throws Exception {
		this.atsdMetricWriter = new AtsdMetricWriter();
		this.atsdMetricWriter.setUsername("u");
		this.atsdMetricWriter.setPassword("p");
		RestTemplate restTemplate = new RestTemplate();
		this.atsdMetricWriter.setRestTemplate(restTemplate);
		this.mockServer = MockRestServiceServer.createServer(restTemplate);
	}

	@Test
	public void testFlush() throws Exception {
		this.atsdMetricWriter.afterPropertiesSet();
		String expectedAuthHeader = "Basic " + new String(Base64Utils.encode("u:p".getBytes()));
		this.mockServer
				.expect(MockRestRequestMatchers.requestTo(AtsdMetricWriter.DEFAULT_URL))
				.andExpect(MockRestRequestMatchers.content().string("series e:atsd-default ms:1000 m:test=10\n"))
				.andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
				.andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, "text/plain"))
				.andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, "application/json"))
				.andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, expectedAuthHeader))
				.andRespond(MockRestResponseCreators.withSuccess("", MediaType.APPLICATION_JSON));

		this.atsdMetricWriter.set(new Metric<Number>("test", 10, new Date(1000)));
		this.atsdMetricWriter.flush();

		this.mockServer.verify();
	}

	@Test
	public void testFlushAutomatically() throws Exception {
		this.atsdMetricWriter.setUrl("/test");
		this.atsdMetricWriter.setBufferSize(2);
		this.atsdMetricWriter.afterPropertiesSet();

		String expectedContent = "series e:atsd-default ms:1000 m:test=10\n" +
				"series e:atsd-default ms:2000 m:test=20\n";
		this.mockServer
				.expect(MockRestRequestMatchers.requestTo("/test"))
				.andExpect(MockRestRequestMatchers.content().string(expectedContent))
				.andRespond(MockRestResponseCreators.withSuccess("", MediaType.APPLICATION_JSON));

		this.atsdMetricWriter.set(new Metric<Number>("test", 10, new Date(1000)));
		this.atsdMetricWriter.set(new Metric<Number>("test", 20, new Date(2000)));
		this.atsdMetricWriter.set(new Metric<Number>("test", 30, new Date(3000)));

		this.mockServer.verify();
	}

	@Test
	public void testAfterPropertiesSetInvalidBufferSize() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Buffer size must be greater than 0");
		this.atsdMetricWriter.setBufferSize(0);
		this.atsdMetricWriter.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetEmptyUsername() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Username is required");
		this.atsdMetricWriter.setUsername("");
		this.atsdMetricWriter.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetEmptyPassword() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Password is required");
		this.atsdMetricWriter.setPassword("");
		this.atsdMetricWriter.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetEmptyUrl() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Url is required");
		this.atsdMetricWriter.setUrl(null);
		this.atsdMetricWriter.afterPropertiesSet();
	}
}
