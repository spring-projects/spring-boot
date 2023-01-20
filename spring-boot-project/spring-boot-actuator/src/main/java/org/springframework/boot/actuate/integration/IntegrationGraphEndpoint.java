/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collection;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.integration.graph.Graph;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.integration.graph.IntegrationNode;
import org.springframework.integration.graph.LinkNode;

/**
 * {@link Endpoint @Endpoint} to expose the Spring Integration graph.
 *
 * @author Tim Ysewyn
 * @since 2.1.0
 */
@Endpoint(id = "integrationgraph")
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
	public GraphDescriptor graph() {
		return new GraphDescriptor(this.graphServer.getGraph());
	}

	@WriteOperation
	public void rebuild() {
		this.graphServer.rebuild();
	}

	/**
	 * Description of a {@link Graph}.
	 */
	public static class GraphDescriptor implements OperationResponseBody {

		private final Map<String, Object> contentDescriptor;

		private final Collection<IntegrationNode> nodes;

		private final Collection<LinkNode> links;

		GraphDescriptor(Graph graph) {
			this.contentDescriptor = graph.getContentDescriptor();
			this.nodes = graph.getNodes();
			this.links = graph.getLinks();
		}

		public Map<String, Object> getContentDescriptor() {
			return this.contentDescriptor;
		}

		public Collection<IntegrationNode> getNodes() {
			return this.nodes;
		}

		public Collection<LinkNode> getLinks() {
			return this.links;
		}

	}

}
