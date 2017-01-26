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

package org.springframework.boot.autoconfigure.cache;

import java.util.List;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.spring.cache.CacheBuilder;
import com.couchbase.client.spring.cache.CouchbaseCacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Couchbase cache configuration.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ Bucket.class, CouchbaseCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@ConditionalOnSingleCandidate(Bucket.class)
@Conditional(CacheCondition.class)
public class CouchbaseCacheConfiguration {

	private final CacheProperties cacheProperties;

	private final CacheManagerCustomizers customizers;

	private final Bucket bucket;

	public CouchbaseCacheConfiguration(CacheProperties cacheProperties,
			CacheManagerCustomizers customizers, Bucket bucket) {
		this.cacheProperties = cacheProperties;
		this.customizers = customizers;
		this.bucket = bucket;
	}

	@Bean
	public CouchbaseCacheManager cacheManager() {
		List<String> cacheNames = this.cacheProperties.getCacheNames();
		CouchbaseCacheManager cacheManager = new CouchbaseCacheManager(
				CacheBuilder.newInstance(this.bucket)
						.withExpiration(this.cacheProperties.getCouchbase()
								.getExpirationSeconds()),
				cacheNames.toArray(new String[cacheNames.size()]));
		return this.customizers.customize(cacheManager);
	}

}
