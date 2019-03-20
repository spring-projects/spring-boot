/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.cache;

import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.infinispan.spring.provider.SpringCache;

/**
 * {@link CacheStatisticsProvider} implementation for Infinispan.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class InfinispanCacheStatisticsProvider
		extends AbstractJmxCacheStatisticsProvider<SpringCache> {

	@Override
	protected ObjectName getObjectName(SpringCache cache)
			throws MalformedObjectNameException {
		ObjectName name = new ObjectName(
				"org.infinispan:component=Statistics,type=Cache,name=\"" + cache.getName()
						+ "(local)\",*");
		Set<ObjectInstance> instances = getMBeanServer().queryMBeans(name, null);
		if (instances.size() == 1) {
			return instances.iterator().next().getObjectName();
		}
		// None or more than one
		return null;
	}

	@Override
	protected CacheStatistics getCacheStatistics(ObjectName objectName) {
		DefaultCacheStatistics statistics = new DefaultCacheStatistics();
		Integer size = getAttribute(objectName, "numberOfEntries", Integer.class);
		if (size != null) {
			statistics.setSize((long) size);
			if (size > 0) {
				// Let's initialize the stats if we have some data
				initializeStats(objectName, statistics);
			}
		}
		return statistics;
	}

	private void initializeStats(ObjectName objectName,
			DefaultCacheStatistics statistics) {
		Double hitRatio = getAttribute(objectName, "hitRatio", Double.class);
		if ((hitRatio != null)) {
			statistics.setHitRatio(hitRatio);
			statistics.setMissRatio(1 - hitRatio);
		}
	}

}
