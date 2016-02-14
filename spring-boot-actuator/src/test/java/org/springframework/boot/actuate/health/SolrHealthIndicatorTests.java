/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
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
		assertEquals(1, this.context.getBeanNamesForType(SolrServer.class).length);
		SolrHealthIndicator healthIndicator = this.context
				.getBean(SolrHealthIndicator.class);
		assertNotNull(healthIndicator);
	}

	@Test
	public void solrIsUp() throws Exception {
		SolrServer solrServer = mock(SolrServer.class);
		SolrPingResponse pingResponse = new SolrPingResponse();
		NamedList<Object> response = new NamedList<Object>();
		response.add("status", "OK");
		pingResponse.setResponse(response);
		given(solrServer.ping()).willReturn(pingResponse);

		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrServer);
		Health health = healthIndicator.health();
		assertEquals(Status.UP, health.getStatus());
		assertEquals("OK", health.getDetails().get("solrStatus"));
	}

	@Test
	public void solrIsDown() throws Exception {
		SolrServer solrServer = mock(SolrServer.class);
		given(solrServer.ping()).willThrow(new IOException("Connection failed"));

		SolrHealthIndicator healthIndicator = new SolrHealthIndicator(solrServer);
		Health health = healthIndicator.health();
		assertEquals(Status.DOWN, health.getStatus());
		assertTrue(((String) health.getDetails().get("error"))
				.contains("Connection failed"));
	}
}
