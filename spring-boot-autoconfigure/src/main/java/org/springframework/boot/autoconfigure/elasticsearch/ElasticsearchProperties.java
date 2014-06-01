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
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.data.elasticsearch")
public class ElasticsearchProperties {

	private String clusterName = "elasticsearch";

	private String clusterNodes;

	private boolean local = true;

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

	public Client createClient() {
		try {
			return (StringUtils.hasLength(this.clusterNodes) ? createTransportClient()
					: createNodeClient());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Client createNodeClient() throws Exception {
		NodeClientFactoryBean factory = new NodeClientFactoryBean(this.local);
		factory.setClusterName(this.clusterName);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	private Client createTransportClient() throws Exception {
		TransportClientFactoryBean factory = new TransportClientFactoryBean();
		factory.setClusterName(this.clusterName);
		factory.setClusterNodes(this.clusterNodes);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

}
