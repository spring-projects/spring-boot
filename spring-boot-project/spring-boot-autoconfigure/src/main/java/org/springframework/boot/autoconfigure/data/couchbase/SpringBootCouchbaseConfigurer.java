/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;

import org.springframework.data.couchbase.config.CouchbaseConfigurer;

/**
 * A simple {@link CouchbaseConfigurer} implementation.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class SpringBootCouchbaseConfigurer implements CouchbaseConfigurer {

	private final CouchbaseEnvironment env;

	private final Cluster cluster;

	private final ClusterInfo clusterInfo;

	private final Bucket bucket;

	public SpringBootCouchbaseConfigurer(CouchbaseEnvironment env, Cluster cluster,
			ClusterInfo clusterInfo, Bucket bucket) {
		this.env = env;
		this.cluster = cluster;
		this.clusterInfo = clusterInfo;
		this.bucket = bucket;
	}

	@Override
	public CouchbaseEnvironment couchbaseEnvironment() throws Exception {
		return this.env;
	}

	@Override
	public Cluster couchbaseCluster() throws Exception {
		return this.cluster;
	}

	@Override
	public ClusterInfo couchbaseClusterInfo() throws Exception {
		return this.clusterInfo;
	}

	@Override
	public Bucket couchbaseClient() throws Exception {
		return this.bucket;
	}

}
