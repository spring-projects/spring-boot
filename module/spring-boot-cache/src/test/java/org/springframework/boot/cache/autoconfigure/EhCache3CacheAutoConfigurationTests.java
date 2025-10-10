/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.cache.autoconfigure;

import javax.cache.CacheManager;

import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.cache.autoconfigure.CacheAutoConfigurationTests.DefaultCacheConfiguration;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheAutoConfiguration} with EhCache 3.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@ClassPathExclusions("ehcache-2*.jar")
class EhCache3CacheAutoConfigurationTests extends AbstractCacheAutoConfigurationTests {

	@Test
	void ehcache3AsJCacheWithCaches() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
			});
	}

	@Test
	@WithResource(name = "ehcache3.xml", content = """
			<config
					xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
					xmlns='http://www.ehcache.org/v3'
					xmlns:jsr107='http://www.ehcache.org/v3/jsr107'
					xsi:schemaLocation="
			        http://www.ehcache.org/v3 https://www.ehcache.org/schema/ehcache-core-3.10.xsd
			        http://www.ehcache.org/v3/jsr107 https://www.ehcache.org/schema/ehcache-107-ext-3.10.xsd">

				<cache-template name="example">
					<heap unit="entries">200</heap>
				</cache-template>

				<cache alias="foo" uses-template="example">
					<expiry>
						<ttl unit="seconds">600</ttl>
					</expiry>
					<jsr107:mbeans enable-statistics="true"/>
				</cache>

				<cache alias="bar" uses-template="example">
					<expiry>
						<ttl unit="seconds">400</ttl>
					</expiry>
				</cache>

			</config>
			""")
	void ehcache3AsJCacheWithConfig() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		String configLocation = "ehcache3.xml";
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.jcache.config=" + configLocation)
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);

				Resource configResource = new ClassPathResource(configLocation);
				CacheManager jCache = cacheManager.getCacheManager();
				assertThat(jCache).isNotNull();
				assertThat(jCache.getURI()).isEqualTo(configResource.getURI());
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
			});
	}

}
