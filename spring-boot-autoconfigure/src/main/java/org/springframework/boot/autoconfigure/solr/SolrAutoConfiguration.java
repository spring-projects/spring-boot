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

package org.springframework.boot.autoconfigure.solr;

import javax.annotation.PreDestroy;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Solr
 *
 * @author Christoph Strobl
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ HttpSolrServer.class, CloudSolrServer.class })
@EnableConfigurationProperties(SolrProperties.class)
public class SolrAutoConfiguration {

	@Autowired
	private SolrProperties properties;

	private SolrServer solrServer;

	@PreDestroy
	public void close() {
		if (this.solrServer != null) {
			this.solrServer.shutdown();
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public SolrServer solrServer() {
		this.solrServer = createSolrServer();
		return this.solrServer;
	}

	private SolrServer createSolrServer() {
		if (StringUtils.hasText(this.properties.getZkHost())) {
			return new CloudSolrServer(this.properties.getZkHost());
		}
		return new HttpSolrServer(this.properties.getHost());
	}

}
