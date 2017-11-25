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

package org.springframework.boot.autoconfigure.couchbase.embedded;

import java.io.IOException;
import java.util.Collections;

import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.mock.Bucket;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Couchbase.
 *
 * @author Alex Derkach
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties({CouchbaseProperties.class})
@AutoConfigureBefore(CouchbaseAutoConfiguration.class)
@ConditionalOnClass({CouchbaseBucket.class, CouchbaseMock.class})
public class EmbeddedCouchbaseAutoConfiguration {

	@Bean(destroyMethod = "stop")
	@ConditionalOnMissingBean
	public CouchbaseMock couchbaseMock(CouchbaseProperties properties) throws IOException, InterruptedException {

		CouchbaseProperties.Bucket bucketProperties = properties.getBucket();

		BucketConfiguration bucketConfiguration = new BucketConfiguration();
		bucketConfiguration.name = bucketProperties.getName();
		bucketConfiguration.password = bucketProperties.getPassword();
		bucketConfiguration.type = Bucket.BucketType.COUCHBASE;
		bucketConfiguration.numReplicas = 1;
		bucketConfiguration.numNodes = 1;

		CouchbaseMock couchbaseMock = new CouchbaseMock(0, Collections.singletonList(bucketConfiguration));
		couchbaseMock.start();
		couchbaseMock.waitForStartup();

		String host = couchbaseMock.getHttpHost();
		int bootstrapHttpDirectPort = couchbaseMock.getHttpPort();
		int bootstrapCarrierDirectPort = couchbaseMock.getBuckets().get(properties.getBucket().getName()).getServers()[0].getPort();

		couchbaseMock.startHarakiriMonitor(host + ":" + bootstrapHttpDirectPort, false);

		properties.setBootstrapHosts(Collections.singletonList(host));
		properties.setBootstrapHttpDirectPort(bootstrapHttpDirectPort);
		properties.setBootstrapCarrierDirectPort(bootstrapCarrierDirectPort);

		return couchbaseMock;
	}

	@Configuration
	public static class EmbeddedCouchbaseConfiguration extends CouchbaseAutoConfiguration.CouchbaseConfiguration {

		public EmbeddedCouchbaseConfiguration(CouchbaseMock couchbaseMock, CouchbaseProperties properties) {
			super(properties);
		}
	}
}
