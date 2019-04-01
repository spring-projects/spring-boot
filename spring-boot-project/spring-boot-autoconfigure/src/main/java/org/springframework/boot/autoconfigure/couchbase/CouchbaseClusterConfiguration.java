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

package org.springframework.boot.autoconfigure.couchbase;

import java.util.List;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Support class to configure Couchbase based on {@link CouchbaseConfiguration}.
 *
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
@Configuration
public class CouchbaseClusterConfiguration {

	private final CouchbaseProperties couchbaseProperties;

	private final CouchbaseEnvironment couchbaseEnvironment;

	public CouchbaseClusterConfiguration(CouchbaseProperties couchbaseProperties,
			DefaultCouchbaseEnvironment couchbaseEnvironment) {
		this.couchbaseProperties = couchbaseProperties;
		this.couchbaseEnvironment = couchbaseEnvironment;
	}

	@Bean
	@Primary
	public Cluster couchbaseCluster() {
		return CouchbaseCluster.create(this.couchbaseEnvironment,
				determineBootstrapHosts());
	}

	/**
	 * Determine the Couchbase nodes to bootstrap from.
	 * @return the Couchbase nodes to bootstrap from
	 */
	protected List<String> determineBootstrapHosts() {
		return this.couchbaseProperties.getBootstrapHosts();
	}

}
