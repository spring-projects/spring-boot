/*
 * Copyright 2012-2017 the original author or authors.
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

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.util.ClassUtils;
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
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put("http.enabled", String.valueOf(false));
		defaults.put("transport.type", "local");
		defaults.put("path.home", System.getProperty("user.dir"));
		DEFAULTS = Collections.unmodifiableMap(defaults);
	}

	private static final Set<String> TRANSPORT_PLUGINS;

	static {
		Set<String> plugins = new LinkedHashSet<>();
		plugins.add("org.elasticsearch.transport.Netty4Plugin");
		plugins.add("org.elasticsearch.transport.Netty3Plugin");
		TRANSPORT_PLUGINS = Collections.unmodifiableSet(plugins);
	}

	private static final Log logger = LogFactory
			.getLog(ElasticsearchAutoConfiguration.class);

	private final ElasticsearchProperties properties;

	private Closeable closeable;

	public ElasticsearchAutoConfiguration(ElasticsearchProperties properties) {
		this.properties = properties;
	}

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
		Settings.Builder settings = Settings.builder();
		for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
			if (!this.properties.getProperties().containsKey(entry.getKey())) {
				settings.put(entry.getKey(), entry.getValue());
			}
		}
		settings.put(this.properties.getProperties());
		settings.put("cluster.name", this.properties.getClusterName());
		Node node = createNode(settings.build());
		this.closeable = node;
		node.start();
		return node.client();
	}

	private Node createNode(Settings settings) {
		Collection<Class<? extends Plugin>> plugins = findPlugins();
		if (plugins.isEmpty()) {
			return new Node(settings);
		}
		return new PluggableNode(settings, plugins);
	}

	@SuppressWarnings("unchecked")
	private Collection<Class<? extends Plugin>> findPlugins() {
		for (String candidate : TRANSPORT_PLUGINS) {
			if (ClassUtils.isPresent(candidate, null)) {
				Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) ClassUtils
						.resolveClassName(candidate, null);
				return Collections.singleton(pluginClass);
			}
		}
		return Collections.emptySet();
	}

	private Client createTransportClient() throws Exception {
		TransportClientFactoryBean factory = new TransportClientFactoryBean();
		factory.setClusterNodes(this.properties.getClusterNodes());
		factory.setProperties(createProperties());
		factory.afterPropertiesSet();
		TransportClient client = factory.getObject();
		this.closeable = client;
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
		if (this.closeable != null) {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Closing Elasticsearch client");
				}
				this.closeable.close();
			}
			catch (final Exception ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Error closing Elasticsearch client: ", ex);
				}
			}
		}
	}

	/**
	 * {@link Node} subclass to support {@link Plugin Plugins}.
	 */
	private static class PluggableNode extends Node {

		PluggableNode(Settings preparedSettings,
				Collection<Class<? extends Plugin>> classpathPlugins) {
			super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null),
					classpathPlugins);
		}

	}

}
