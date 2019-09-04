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

import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import org.springframework.beans.factory.BeanCreationException;

/**
 * {@link Extension} for working with an optional Couchbase server. Expects a default
 * {@link Bucket} with no password to be available on localhost.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class LocalCouchbaseServer implements ExecutionCondition, TestExecutionExceptionHandler {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		try {
			CouchbaseEnvironment environment = DefaultCouchbaseEnvironment.create();
			Cluster cluster = CouchbaseCluster.create(environment, "localhost");
			testConnection(cluster);
			cluster.disconnect();
			environment.shutdownAsync();
			return ConditionEvaluationResult.enabled("Local Couchbase server available");
		}
		catch (Exception ex) {
			return ConditionEvaluationResult.disabled("Local Couchbase server not available");
		}
	}

	private static void testConnection(Cluster cluster) {
		Bucket bucket = cluster.openBucket(2, TimeUnit.SECONDS);
		bucket.close();
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable ex) throws Throwable {
		if ((ex instanceof BeanCreationException)
				&& "couchbaseClient".equals(((BeanCreationException) ex).getBeanName())) {
			return;
		}
		throw ex;
	}

}
