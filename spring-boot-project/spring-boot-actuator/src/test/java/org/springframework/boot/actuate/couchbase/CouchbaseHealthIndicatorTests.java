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

package org.springframework.boot.actuate.couchbase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.util.features.Version;
import org.junit.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.couchbase.core.CouchbaseOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CouchbaseHealthIndicator}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class CouchbaseHealthIndicatorTests {

	@Test
	public void couchbaseIsUp() throws UnknownHostException {
		BucketInfo bucketInfo = mock(BucketInfo.class);
		given(bucketInfo.nodeList()).willReturn(
				Collections.singletonList(InetAddress.getByName("127.0.0.1")));
		BucketManager bucketManager = mock(BucketManager.class);
		given(bucketManager.info()).willReturn(bucketInfo);
		Bucket bucket = mock(Bucket.class);
		given(bucket.bucketManager()).willReturn(bucketManager);
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		given(clusterInfo.getAllVersions())
				.willReturn(Collections.singletonList(new Version(1, 2, 3)));
		CouchbaseOperations couchbaseOperations = mock(CouchbaseOperations.class);
		given(couchbaseOperations.getCouchbaseBucket()).willReturn(bucket);
		given(couchbaseOperations.getCouchbaseClusterInfo()).willReturn(clusterInfo);
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(
				couchbaseOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnly(entry("versions", "1.2.3"),
				entry("nodes", "/127.0.0.1"));
		verify(clusterInfo).getAllVersions();
		verify(bucketInfo).nodeList();
	}

	@Test
	public void couchbaseIsDown() {
		CouchbaseOperations couchbaseOperations = mock(CouchbaseOperations.class);
		given(couchbaseOperations.getCouchbaseClusterInfo())
				.willThrow(new IllegalStateException("test, expected"));
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(
				couchbaseOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("test, expected");
		verify(couchbaseOperations).getCouchbaseClusterInfo();
	}

}
