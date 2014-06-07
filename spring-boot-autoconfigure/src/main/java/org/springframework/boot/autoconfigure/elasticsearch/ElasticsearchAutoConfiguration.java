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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Elasticsearch.
 * 
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ Client.class, TransportClientFactoryBean.class,
		NodeClientFactoryBean.class })
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchAutoConfiguration implements DisposableBean {

	private static Log logger = LogFactory.getLog(ElasticsearchAutoConfiguration.class);

	@Autowired
	private ElasticsearchProperties properties;

	private Client client;

	@Bean
	public Client elasticsearchClient() {
		try {
			this.client = createClient();
			return this.client;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Client createClient() throws Exception {
		if (StringUtils.hasLength(this.properties.getClusterNodes())) {
			return createTransportClient();
		}
		return createNodeClient();
	}

	private Client createNodeClient() throws Exception {
		NodeClientFactoryBean factory = new NodeClientFactoryBean(true);
		factory.setClusterName(this.properties.getClusterName());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	private Client createTransportClient() throws Exception {
		TransportClientFactoryBean factory = new TransportClientFactoryBean();
		factory.setClusterName(this.properties.getClusterName());
		factory.setClusterNodes(this.properties.getClusterNodes());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	@Override
	public void destroy() throws Exception {
		if (this.client != null) {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Closing Elasticsearch client");
				}
				if (this.client != null) {
					this.client.close();
				}
			}
			catch (final Exception ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Error closing Elasticsearch client: ", ex);
				}
			}
		}
	}

}
