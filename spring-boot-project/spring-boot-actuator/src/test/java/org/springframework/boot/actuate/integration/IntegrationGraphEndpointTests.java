/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.integration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.integration.graph.Graph;
import org.springframework.integration.graph.IntegrationGraphServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link IntegrationGraphEndpoint}.
 *
 * @author Tim Ysewyn
 */
public class IntegrationGraphEndpointTests {

	@Mock
	private IntegrationGraphServer integrationGraphServer;

	@InjectMocks
	private IntegrationGraphEndpoint integrationGraphEndpoint;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void readOperationShouldReturnGraph() {
		Graph mockedGraph = mock(Graph.class);
		given(this.integrationGraphServer.getGraph()).willReturn(mockedGraph);
		Graph graph = this.integrationGraphEndpoint.graph();
		verify(this.integrationGraphServer).getGraph();
		assertThat(graph).isEqualTo(mockedGraph);
	}

	@Test
	public void writeOperationShouldRebuildGraph() {
		this.integrationGraphEndpoint.rebuild();
		verify(this.integrationGraphServer).rebuild();
	}

}
