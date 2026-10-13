/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.integration.actuate.endpoint.IntegrationGraphEndpoint.GraphDescriptor;
import org.springframework.integration.graph.Graph;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.integration.graph.IntegrationNode;
import org.springframework.integration.graph.LinkNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntegrationGraphEndpoint}.
 *
 * @author Tim Ysewyn
 * @author Moritz Halbritter
 */
class IntegrationGraphEndpointTests {

	private final IntegrationGraphServer server = mock(IntegrationGraphServer.class);

	private final IntegrationGraphEndpoint endpoint = new IntegrationGraphEndpoint(this.server);

	@Test
	void readOperationShouldReturnGraph() {
		Graph graph = mock(Graph.class);
		Map<String, Object> contentDescriptor = new LinkedHashMap<>();
		Collection<IntegrationNode> nodes = new ArrayList<>();
		Collection<LinkNode> links = new ArrayList<>();
		given(graph.contentDescriptor()).willReturn(contentDescriptor);
		given(graph.nodes()).willReturn(nodes);
		given(graph.links()).willReturn(links);
		given(this.server.getGraph()).willReturn(graph);
		GraphDescriptor descriptor = this.endpoint.graph();
		then(this.server).should().getGraph();
		assertThat(descriptor.getContentDescriptor()).isSameAs(contentDescriptor);
		assertThat(descriptor.getNodes()).isSameAs(nodes);
		assertThat(descriptor.getLinks()).isSameAs(links);
	}

	@Test
	void writeOperationShouldRebuildGraph() {
		this.endpoint.rebuild();
		then(this.server).should().rebuild();
	}

}
