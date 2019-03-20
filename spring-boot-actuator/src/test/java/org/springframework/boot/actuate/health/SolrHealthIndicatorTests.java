/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SolrHealthIndicator}
 *
 * @author Andy Wilkinson
 */
public class SolrHealthIndicatorTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void indicatorExists() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class, SolrAutoConfiguration.class,
				EndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(SolrClient.class).length)
				.isEqualTo(1);
		SolrHealthIndicator healthIndicator = this.context
				.getBean(SolrHealthIndicator.class);
		assertThat(healthIndicator).isNotNull();
	}

	@Test
	public void solrIsUp() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), (String) isNull()))
				.willReturn(mockResponse(0));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("solrStatus")).isEqualTo("OK");
	}

	@Test
	public void solrIsUpAndRequestFailed() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), (String) isNull()))
				.willReturn(mockResponse(400));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("solrStatus")).isEqualTo(400);
	}

	@Test
	public void solrIsDown() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), (String) isNull()))
				.willThrow(new IOException("Connection failed"));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error"))
				.contains("Connection failed");
	}

	private NamedList<Object> mockResponse(int status) {
		NamedList<Object> response = new NamedList<Object>();
		NamedList<Object> headers = new NamedList<Object>();
		headers.add("status", status);
		response.add("responseHeader", headers);
		return response;
	}

}
