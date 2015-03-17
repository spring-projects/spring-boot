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

package org.springframework.boot.autoconfigure.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Elasticsearch.
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.data.elasticsearch")
public class ElasticsearchProperties {

	/**
	 * Elasticsearch cluster name.
	 */
	private String clusterName = "elasticsearch";

	/**
	 * Comma-separated list of cluster node addresses. If not specified, starts a client
	 * node.
	 */
	private String clusterNodes;

	/**
	 * Allow the client to sniff for other members of the cluster.
	 */
	private boolean clientTransportSniff = true;

	public String getClusterName() {
		return this.clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getClusterNodes() {
		return this.clusterNodes;
	}

	public void setClusterNodes(String clusterNodes) {
		this.clusterNodes = clusterNodes;
	}

	public boolean getClientTransportSniff() {
		return this.clientTransportSniff;
	}

	public void setClientTransportSniff(boolean clientTransportSniff) {
		this.clientTransportSniff = clientTransportSniff;
	}

}
