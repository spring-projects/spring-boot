/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.util.ReflectionUtils;
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

	private static final Map<String, String> DEFAULTS;

	static {
		Map<String, String> defaults = new LinkedHashMap<String, String>();
		defaults.put("http.enabled", String.valueOf(false));
		defaults.put("node.local", String.valueOf(true));
		DEFAULTS = Collections.unmodifiableMap(defaults);
	}

	private static Log logger = LogFactory.getLog(ElasticsearchAutoConfiguration.class);

	@Autowired
	private ElasticsearchProperties properties;

	private Releasable releasable;

	@Bean
	@ConditionalOnMissingBean
	public Client elasticsearchClient() {
		try {
			return createClient();
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
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
		for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
			if (!this.properties.getProperties().containsKey(entry.getKey())) {
				settings.put(entry.getKey(), entry.getValue());
			}
		}
		settings.put(this.properties.getProperties());
		Node node = new NodeBuilder().settings(settings)
				.clusterName(this.properties.getClusterName()).node();
		this.releasable = node;
		return node.client();
	}

	private Client createTransportClient() throws Exception {
		TransportClientFactoryBean factory = new TransportClientFactoryBean();
		factory.setClusterNodes(this.properties.getClusterNodes());
		factory.setProperties(createProperties());
		factory.afterPropertiesSet();
		TransportClient client = factory.getObject();
		this.releasable = client;
		return client;
	}

	private Properties createProperties() {
		Properties properties = new Properties();
		properties.put("cluster.name", this.properties.getClusterName());
		properties.putAll(this.properties.getProperties());
		return properties;
	}

	@Override
	public void destroy() throws Exception {
		if (this.releasable != null) {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Closing Elasticsearch client");
				}
				try {
					this.releasable.close();
				}
				catch (NoSuchMethodError ex) {
					// Earlier versions of Elasticsearch had a different method name
					ReflectionUtils.invokeMethod(
							ReflectionUtils.findMethod(Releasable.class, "release"),
							this.releasable);
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
