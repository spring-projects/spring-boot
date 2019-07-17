/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.solr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for Solr.
 *
 * @author Christoph Strobl
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.data.solr")
public class SolrProperties {

	/**
	 * Solr host. Ignored if "zk-host" is set.
	 */
	private String host = "http://127.0.0.1:8983/solr";

	/**
	 * ZooKeeper host address in the form HOST:PORT.
	 */
	private String zkHost;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getZkHost() {
		return this.zkHost;
	}

	public void setZkHost(String zkHost) {
		this.zkHost = zkHost;
	}

}
