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

package org.springframework.boot.autoconfigure.cache;

import java.time.Duration;
import java.util.List;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.spring.cache.CacheBuilder;
import com.couchbase.client.spring.cache.CouchbaseCacheManager;

import org.springframework.boot.autoconfigure.cache.CacheProperties.Couchbase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Couchbase cache configuration.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Bucket.class, CouchbaseCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@ConditionalOnSingleCandidate(Bucket.class)
@Conditional(CacheCondition.class)
public class CouchbaseCacheConfiguration {

	@Bean
	public CouchbaseCacheManager cacheManager(CacheProperties cacheProperties,
			CacheManagerCustomizers customizers, Bucket bucket) {
		List<String> cacheNames = cacheProperties.getCacheNames();
		CacheBuilder builder = CacheBuilder.newInstance(bucket);
		Couchbase couchbase = cacheProperties.getCouchbase();
		PropertyMapper.get().from(couchbase::getExpiration).whenNonNull()
				.asInt(Duration::getSeconds).to(builder::withExpiration);
		String[] names = StringUtils.toStringArray(cacheNames);
		CouchbaseCacheManager cacheManager = new CouchbaseCacheManager(builder, names);
		return customizers.customize(cacheManager);
	}

}
