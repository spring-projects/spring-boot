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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.integration.IntegrationGraphEndpoint.IntegrationGraphEndpointRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.aot.BindingReflectionHintsRegistrar;
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

/**
 * {@link Endpoint @Endpoint} to expose the Spring Integration graph.
 *
 * @author Tim Ysewyn
 * @since 2.1.0
 */
@Endpoint(id = "integrationgraph")
@ImportRuntimeHints(IntegrationGraphEndpointRuntimeHints.class)
public class IntegrationGraphEndpoint {

	private final IntegrationGraphServer graphServer;

	/**
	 * Create a new {@code IntegrationGraphEndpoint} instance that exposes a graph
	 * containing all the Spring Integration components in the given
	 * {@link IntegrationGraphServer}.
	 * @param graphServer the integration graph server
	 */
	public IntegrationGraphEndpoint(IntegrationGraphServer graphServer) {
		this.graphServer = graphServer;
	}

	@ReadOperation
	public Graph graph() {
		return this.graphServer.getGraph();
	}

	@WriteOperation
	public void rebuild() {
		this.graphServer.rebuild();
	}

	static class IntegrationGraphEndpointRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), Graph.class,
					CompositeMessageHandlerNode.class, DiscardingMessageHandlerNode.class, EndpointNode.class,
					ErrorCapableCompositeMessageHandlerNode.class, ErrorCapableDiscardingMessageHandlerNode.class,
					ErrorCapableEndpointNode.class, ErrorCapableMessageHandlerNode.class, ErrorCapableRoutingNode.class,
					MessageChannelNode.class, MessageGatewayNode.class, MessageHandlerNode.class,
					MessageProducerNode.class, MessageSourceNode.class, PollableChannelNode.class,
					RoutingMessageHandlerNode.class);
		}

	}

}
