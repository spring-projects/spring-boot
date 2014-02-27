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

import org.elasticsearch.client.Client;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Elasticsearch.
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 */
@ConfigurationProperties(name = "spring.data.elasticsearch")
public class ElasticsearchProperties {

	private NodeClientFactoryBean nodeFactory;

	private TransportClientFactoryBean transportFactory;

	private String clusterName = "spring-boot-auto-cluster";

	private String clusterNodes;

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getClusterNodes() {
		return clusterNodes;
	}

	public void setClusterNodes(String clusterNodes) {
		this.clusterNodes = clusterNodes;
	}

	public Client createClient() throws Exception {
		return StringUtils.isEmpty(clusterNodes) ? createNodeClient() : createTransportClient();
	}

	private Client createNodeClient() throws Exception {
		if (nodeFactory == null) {
			nodeFactory = new NodeClientFactoryBean(true);
			nodeFactory.setClusterName(clusterName);
			nodeFactory.afterPropertiesSet();
		}
		return nodeFactory.getObject();
	}

	private Client createTransportClient() throws Exception {
		if (transportFactory == null) {
			transportFactory = new TransportClientFactoryBean();
			transportFactory.setClusterName(clusterName);
			transportFactory.setClusterNodes(clusterNodes);
			transportFactory.afterPropertiesSet();
		}
		return transportFactory.getObject();
	}
}
