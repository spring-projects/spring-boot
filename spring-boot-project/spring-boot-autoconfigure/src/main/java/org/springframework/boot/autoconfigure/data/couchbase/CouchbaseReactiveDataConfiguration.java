/*
 * Copyright 2012-2020 the original author or authors.
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
import com.couchbase.client.java.cluster.ClusterInfo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.RxJavaCouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.core.convert.translation.TranslationService;
import org.springframework.data.couchbase.repository.config.ReactiveRepositoryOperationsMapping;

/**
 * Configuration for Spring Data's couchbase reactive support.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class CouchbaseReactiveDataConfiguration {

	@Bean(name = BeanNames.RXJAVA1_COUCHBASE_TEMPLATE)
	@ConditionalOnMissingBean(name = BeanNames.RXJAVA1_COUCHBASE_TEMPLATE)
	@ConditionalOnBean({ ClusterInfo.class, Bucket.class })
	RxJavaCouchbaseTemplate reactiveCouchbaseTemplate(CouchbaseDataProperties properties, ClusterInfo clusterInfo,
			Bucket bucket, MappingCouchbaseConverter mappingCouchbaseConverter, TranslationService translationService) {
		RxJavaCouchbaseTemplate template = new RxJavaCouchbaseTemplate(clusterInfo, bucket, mappingCouchbaseConverter,
				translationService);
		template.setDefaultConsistency(properties.getConsistency());
		return template;
	}

	@Bean(name = BeanNames.REACTIVE_COUCHBASE_OPERATIONS_MAPPING)
	@ConditionalOnMissingBean(name = BeanNames.REACTIVE_COUCHBASE_OPERATIONS_MAPPING)
	@ConditionalOnSingleCandidate(RxJavaCouchbaseTemplate.class)
	ReactiveRepositoryOperationsMapping reactiveRepositoryOperationsMapping(
			RxJavaCouchbaseTemplate reactiveCouchbaseTemplate) {
		return new ReactiveRepositoryOperationsMapping(reactiveCouchbaseTemplate);
	}

}
