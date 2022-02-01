/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link SolrHealthIndicator}
 *
 * @author Andy Wilkinson
 * @author Markus Schuch
 * @author Phillip Webb
 */
class SolrHealthIndicatorTests {

	@Test
	void healthWhenSolrStatusUpAndBaseUrlPointsToRootReturnsUp() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), isNull())).willReturn(mockResponse(0));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		assertHealth(healthIndicator, Status.UP, 0, "root");
		then(solrClient).should().request(any(CoreAdminRequest.class), isNull());
		then(solrClient).shouldHaveNoMoreInteractions();
	}

	@Test
	void healthWhenSolrStatusDownAndBaseUrlPointsToRootReturnsDown() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), isNull())).willReturn(mockResponse(400));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		assertHealth(healthIndicator, Status.DOWN, 400, "root");
		then(solrClient).should().request(any(CoreAdminRequest.class), isNull());
		then(solrClient).shouldHaveNoMoreInteractions();
	}

	@Test
	void healthWhenSolrStatusUpAndBaseUrlPointsToParticularCoreReturnsUp() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), isNull()))
				.willThrow(new RemoteSolrException("mock", 404, "", null));
		given(solrClient.ping()).willReturn(mockPingResponse(0));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		assertHealth(healthIndicator, Status.UP, 0, "particular core");
		then(solrClient).should().request(any(CoreAdminRequest.class), isNull());
		then(solrClient).should().ping();
		then(solrClient).shouldHaveNoMoreInteractions();
	}

	@Test
	void healthWhenSolrStatusDownAndBaseUrlPointsToParticularCoreReturnsDown() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), isNull()))
				.willThrow(new RemoteSolrException("mock", 404, "", null));
		given(solrClient.ping()).willReturn(mockPingResponse(400));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		assertHealth(healthIndicator, Status.DOWN, 400, "particular core");
		then(solrClient).should().request(any(CoreAdminRequest.class), isNull());
		then(solrClient).should().ping();
		then(solrClient).shouldHaveNoMoreInteractions();
	}

	@Test
	void healthWhenSolrConnectionFailsReturnsDown() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), isNull()))
				.willThrow(new IOException("Connection failed"));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection failed");
		then(solrClient).should().request(any(CoreAdminRequest.class), isNull());
		then(solrClient).shouldHaveNoMoreInteractions();
	}

	@Test
	void healthWhenMakingMultipleCallsRemembersStatusStrategy() throws Exception {
		SolrClient solrClient = mock(SolrClient.class);
		given(solrClient.request(any(CoreAdminRequest.class), isNull()))
				.willThrow(new RemoteSolrException("mock", 404, "", null));
		given(solrClient.ping()).willReturn(mockPingResponse(0));
		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrClient);
		healthIndicator.health();
		then(solrClient).should().request(any(CoreAdminRequest.class), isNull());
		then(solrClient).should().ping();
		then(solrClient).shouldHaveNoMoreInteractions();
		healthIndicator.health();
		then(solrClient).should(times(2)).ping();
		then(solrClient).shouldHaveNoMoreInteractions();
	}

	private void assertHealth(SolrHealthIndicator healthIndicator, Status expectedStatus, int expectedStatusCode,
			String expectedPathType) {
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(expectedStatus);
		assertThat(health.getDetails().get("status")).isEqualTo(expectedStatusCode);
		assertThat(health.getDetails().get("detectedPathType")).isEqualTo(expectedPathType);
	}

	private NamedList<Object> mockResponse(int status) {
		NamedList<Object> response = new NamedList<>();
		NamedList<Object> headers = new NamedList<>();
		headers.add("status", status);
		response.add("responseHeader", headers);
		return response;
	}

	private SolrPingResponse mockPingResponse(int status) {
		SolrPingResponse pingResponse = new SolrPingResponse();
		pingResponse.setResponse(mockResponse(status));
		return pingResponse;
	}

}
