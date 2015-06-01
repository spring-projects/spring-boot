/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.cache;

import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.springframework.cache.jcache.JCacheCache;

/**
 * {@link CacheStatisticsProvider} implementation for a JSR-107 compliant cache.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class JCacheCacheStatisticsProvider extends AbstractJmxCacheStatisticsProvider<JCacheCache> {


	protected ObjectName getObjectName(JCacheCache cache)
			throws MalformedObjectNameException {

		Set<ObjectInstance> instances = getMBeanServer().queryMBeans(
				new ObjectName("javax.cache:type=CacheStatistics,Cache="
						+ cache.getName() + ",*"), null);
		if (instances.size() == 1) {
			return instances.iterator().next().getObjectName();
		}
		return null; // None or more than one
	}

	protected CacheStatistics getCacheStatistics(ObjectName objectName) {
		DefaultCacheStatistics statistics = new DefaultCacheStatistics();
		Float hitPercentage = getAttribute(objectName, "CacheHitPercentage",
				Float.class);
		Float missPercentage = getAttribute(objectName,
				"CacheMissPercentage", Float.class);
		if ((hitPercentage != null && missPercentage != null)
				&& (hitPercentage > 0 || missPercentage > 0)) {
			statistics.setHitRatio(hitPercentage / (double) 100);
			statistics.setMissRatio(missPercentage / (double) 100);
		}
		return statistics;
	}

}
