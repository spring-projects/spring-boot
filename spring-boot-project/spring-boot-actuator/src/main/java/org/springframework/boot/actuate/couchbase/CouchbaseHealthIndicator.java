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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.cluster.ClusterInfo;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link HealthIndicator} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CouchbaseHealthIndicator extends AbstractHealthIndicator {

	private final CouchbaseOperations operations;

	private final long timeout;

	/**
	 * Create an indicator with the specified {@link CouchbaseOperations} and
	 * {@code timeout}.
	 * @param couchbaseOperations the couchbase operations
	 * @param timeout the request timeout
	 */
	public CouchbaseHealthIndicator(CouchbaseOperations couchbaseOperations,
			Duration timeout) {
		super("Couchbase health check failed");
		Assert.notNull(couchbaseOperations, "CouchbaseOperations must not be null");
		Assert.notNull(timeout, "Timeout must not be null");
		this.operations = couchbaseOperations;
		this.timeout = timeout.toMillis();
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		ClusterInfo cluster = this.operations.getCouchbaseClusterInfo();
		BucketInfo bucket = getBucketInfo();
		String versions = StringUtils
				.collectionToCommaDelimitedString(cluster.getAllVersions());
		String nodes = StringUtils.collectionToCommaDelimitedString(bucket.nodeList());
		builder.up().withDetail("versions", versions).withDetail("nodes", nodes);
	}

	private BucketInfo getBucketInfo() throws Exception {
		try {
			return this.operations.getCouchbaseBucket().bucketManager().info(this.timeout,
					TimeUnit.MILLISECONDS);
		}
		catch (RuntimeException ex) {
			if (ex.getCause() instanceof TimeoutException) {
				throw (TimeoutException) ex.getCause();
			}
			throw ex;
		}
	}

}
