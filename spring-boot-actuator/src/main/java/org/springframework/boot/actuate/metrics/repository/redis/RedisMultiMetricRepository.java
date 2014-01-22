/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.metrics.repository.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MultiMetricRepository;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.util.Assert;

/**
 * {@link MultiMetricRepository} implementation backed by a redis store. Metric values are
 * stored as regular values against a key composed of the group name prefixed with a
 * constant prefix (default "spring.groups."). The group names are stored as a zset under
 * <code>[prefix]</code> + "keys".
 * 
 * @author Dave Syer
 */
public class RedisMultiMetricRepository implements MultiMetricRepository {

	private static final String DEFAULT_METRICS_PREFIX = "spring.groups.";

	private String prefix = DEFAULT_METRICS_PREFIX;

	private String keys = this.prefix + "keys";

	private final BoundZSetOperations<String, String> zSetOperations;

	private final RedisOperations<String, String> redisOperations;

	public RedisMultiMetricRepository(RedisConnectionFactory redisConnectionFactory) {
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		this.redisOperations = RedisUtils.stringTemplate(redisConnectionFactory);
		this.zSetOperations = this.redisOperations.boundZSetOps(this.keys);
	}

	/**
	 * The prefix for all metrics keys.
	 * @param prefix the prefix to set for all metrics keys
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
		this.keys = this.prefix + "keys";
	}

	@Override
	public Iterable<Metric<?>> findAll(String metricNamePrefix) {

		BoundZSetOperations<String, String> zSetOperations = this.redisOperations
				.boundZSetOps(keyFor(metricNamePrefix));

		Set<String> keys = zSetOperations.range(0, -1);
		Iterator<String> keysIt = keys.iterator();

		List<Metric<?>> result = new ArrayList<Metric<?>>(keys.size());
		List<String> values = this.redisOperations.opsForValue().multiGet(keys);
		for (String v : values) {
			result.add(deserialize(keysIt.next(), v));
		}
		return result;

	}

	@Override
	public void save(String group, Collection<Metric<?>> values) {
		String groupKey = keyFor(group);
		trackMembership(groupKey);
		BoundZSetOperations<String, String> zSetOperations = this.redisOperations
				.boundZSetOps(groupKey);
		for (Metric<?> metric : values) {
			String raw = serialize(metric);
			String key = keyFor(metric.getName());
			zSetOperations.add(key, 0.0D);
			this.redisOperations.opsForValue().set(key, raw);
		}
	}

	@Override
	public Iterable<String> groups() {
		Set<String> range = this.zSetOperations.range(0, -1);
		Collection<String> result = new ArrayList<String>();
		for (String key : range) {
			result.add(nameFor(key));
		}
		return range;
	}

	@Override
	public long count() {
		return this.zSetOperations.size();
	}

	@Override
	public void reset(String group) {
		String groupKey = keyFor(group);
		if (this.redisOperations.hasKey(groupKey)) {
			BoundZSetOperations<String, String> zSetOperations = this.redisOperations
					.boundZSetOps(groupKey);
			Set<String> keys = zSetOperations.range(0, -1);
			for (String key : keys) {
				this.redisOperations.delete(key);
			}
			this.redisOperations.delete(groupKey);
		}
		this.zSetOperations.remove(groupKey);
	}

	private Metric<?> deserialize(String redisKey, String v) {
		String[] vals = v.split("@");
		Double value = Double.valueOf(vals[0]);
		Date timestamp = vals.length > 1 ? new Date(Long.valueOf(vals[1])) : new Date();
		return new Metric<Double>(nameFor(redisKey), value, timestamp);
	}

	private String serialize(Metric<?> entity) {
		return String.valueOf(entity.getValue() + "@" + entity.getTimestamp().getTime());
	}

	private String keyFor(String name) {
		return this.prefix + name;
	}

	private String nameFor(String redisKey) {
		Assert.state(redisKey != null && redisKey.startsWith(this.prefix),
				"Invalid key does not start with prefix: " + redisKey);
		return redisKey.substring(this.prefix.length());
	}

	private void trackMembership(String redisKey) {
		this.zSetOperations.add(redisKey, 0.0D);
	}

}
