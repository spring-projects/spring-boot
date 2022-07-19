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

package org.springframework.boot.actuate.integration;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.integration.IntegrationGraphEndpoint.IntegrationGraphEndpointRuntimeHints;
import org.springframework.integration.graph.CompositeMessageHandlerNode;
import org.springframework.integration.graph.DiscardingMessageHandlerNode;
import org.springframework.integration.graph.EndpointNode;
import org.springframework.integration.graph.ErrorCapableCompositeMessageHandlerNode;
import org.springframework.integration.graph.ErrorCapableDiscardingMessageHandlerNode;
import org.springframework.integration.graph.ErrorCapableEndpointNode;
import org.springframework.integration.graph.ErrorCapableMessageHandlerNode;
import org.springframework.integration.graph.ErrorCapableRoutingNode;
import org.springframework.integration.graph.Graph;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.integration.graph.MessageChannelNode;
import org.springframework.integration.graph.MessageGatewayNode;
import org.springframework.integration.graph.MessageHandlerNode;
import org.springframework.integration.graph.MessageProducerNode;
import org.springframework.integration.graph.MessageSourceNode;
import org.springframework.integration.graph.PollableChannelNode;
import org.springframework.integration.graph.RoutingMessageHandlerNode;

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
		Graph mockedGraph = mock(Graph.class);
		given(this.server.getGraph()).willReturn(mockedGraph);
		Graph graph = this.endpoint.graph();
		then(this.server).should().getGraph();
		assertThat(graph).isEqualTo(mockedGraph);
	}

	@Test
	void writeOperationShouldRebuildGraph() {
		this.endpoint.rebuild();
		then(this.server).should().rebuild();
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new IntegrationGraphEndpointRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		Set<Class<?>> bindingTypes = Set.of(Graph.class, EndpointNode.class, CompositeMessageHandlerNode.class,
				DiscardingMessageHandlerNode.class, ErrorCapableCompositeMessageHandlerNode.class,
				ErrorCapableDiscardingMessageHandlerNode.class, ErrorCapableEndpointNode.class,
				ErrorCapableMessageHandlerNode.class, ErrorCapableRoutingNode.class, MessageGatewayNode.class,
				MessageProducerNode.class, PollableChannelNode.class, MessageChannelNode.class,
				MessageHandlerNode.class, MessageSourceNode.class, RoutingMessageHandlerNode.class);
		for (Class<?> bindingType : bindingTypes) {
			assertThat(RuntimeHintsPredicates.reflection().onType(bindingType)
					.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
							.accepts(runtimeHints);
		}
	}

}
