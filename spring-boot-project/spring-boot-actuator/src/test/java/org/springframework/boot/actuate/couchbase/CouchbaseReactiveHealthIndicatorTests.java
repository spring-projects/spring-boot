/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.util.features.Version;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Observable;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.couchbase.core.RxJavaCouchbaseOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CouchbaseReactiveHealthIndicator}.
 */
public class CouchbaseReactiveHealthIndicatorTests {

	@Test
	public void testCouchbaseIsUp() {
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		RxJavaCouchbaseOperations rxJavaCouchbaseOperations = mock(
				RxJavaCouchbaseOperations.class);
		given(rxJavaCouchbaseOperations.getCouchbaseClusterInfo())
				.willReturn(clusterInfo);
		given(clusterInfo.getAllVersions())
				.willReturn(Arrays.asList(new Version(5, 5, 0), new Version(6, 0, 0)));
		Bucket bucket = mock(Bucket.class);
		BucketManager bucketManager = mock(BucketManager.class);
		AsyncBucketManager asyncBucketManager = mock(AsyncBucketManager.class);
		given(rxJavaCouchbaseOperations.getCouchbaseBucket()).willReturn(bucket);
		given(bucket.bucketManager()).willReturn(bucketManager);
		given(bucketManager.async()).willReturn(asyncBucketManager);
		BucketInfo info = mock(BucketInfo.class);
		given(asyncBucketManager.info()).willReturn(Observable.just(info));

		InetAddress node1Address = mock(InetAddress.class);
		InetAddress node2Address = mock(InetAddress.class);
		given(info.nodeList()).willReturn(Arrays.asList(node1Address, node2Address));
		given(node1Address.toString()).willReturn("127.0.0.1");
		given(node2Address.toString()).willReturn("127.0.0.2");

		CouchbaseReactiveHealthIndicator couchbaseReactiveHealthIndicator = new CouchbaseReactiveHealthIndicator(
				rxJavaCouchbaseOperations);

		Mono<Health> health = couchbaseReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsKeys("versions", "nodes");
			assertThat(h.getDetails().get("versions")).isEqualTo("5.5.0,6.0.0");
			assertThat(h.getDetails().get("nodes")).isEqualTo("127.0.0.1,127.0.0.2");
		}).verifyComplete();
	}

	@Test
	public void testCouchbaseIsDown() {
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		RxJavaCouchbaseOperations rxJavaCouchbaseOperations = mock(
				RxJavaCouchbaseOperations.class);
		given(rxJavaCouchbaseOperations.getCouchbaseClusterInfo())
				.willReturn(clusterInfo);
		given(clusterInfo.getAllVersions())
				.willReturn(Collections.singletonList(new Version(5, 5, 0)));
		BucketManager bucketManager = mock(BucketManager.class);
		AsyncBucketManager asyncBucketManager = mock(AsyncBucketManager.class);
		Bucket bucket = mock(Bucket.class);
		given(rxJavaCouchbaseOperations.getCouchbaseBucket()).willReturn(bucket);
		given(bucket.bucketManager()).willReturn(bucketManager);
		given(bucketManager.async()).willReturn(asyncBucketManager);
		given(asyncBucketManager.info())
				.willReturn(Observable.error(new TranscodingException("Failure")));
		CouchbaseReactiveHealthIndicator couchbaseReactiveHealthIndicator = new CouchbaseReactiveHealthIndicator(
				rxJavaCouchbaseOperations);

		Mono<Health> health = couchbaseReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails()).containsOnlyKeys("error");
			assertThat(h.getDetails().get("error"))
					.isEqualTo(TranscodingException.class.getName() + ": Failure");
		}).verifyComplete();
	}

}
