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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;

import org.springframework.data.couchbase.config.CouchbaseConfigurer;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.mock;

/**
 * Test configurer for couchbase that mocks access.
 *
 * @author Stephane Nicoll
 */
@Component
public class CouchbaseTestConfigurer implements CouchbaseConfigurer {

	@Override
	public CouchbaseEnvironment couchbaseEnvironment() throws Exception {
		return mock(CouchbaseEnvironment.class);
	}

	@Override
	public Cluster couchbaseCluster() throws Exception {
		return mock(Cluster.class);
	}

	@Override
	public ClusterInfo couchbaseClusterInfo() {
		return mock(ClusterInfo.class);
	}

	@Override
	public Bucket couchbaseClient() {
		return mock(CouchbaseBucket.class);
	}

}
