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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;

/**
 * A {@link MetricRepository} implementation for a redis backend. Metric values are stored
 * as regular hash values against a key composed of the metric name prefixed with a
 * constant (default "spring.metrics.").
 * 
 * @author Dave Syer
 */
public class RedisMetricRepository implements MetricRepository {

	private static final String DEFAULT_METRICS_PREFIX = "spring.metrics.";

	private String prefix = DEFAULT_METRICS_PREFIX;

	private String keys = this.prefix + "keys";

	private BoundZSetOperations<String, String> zSetOperations;

	private RedisOperations<String, String> redisOperations;

	private ValueOperations<String, Long> longOperations;

	public RedisMetricRepository(RedisConnectionFactory redisConnectionFactory) {
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		this.redisOperations = RedisUtils.stringTemplate(redisConnectionFactory);
		RedisTemplate<String, Long> longRedisTemplate = RedisUtils.createRedisTemplate(
				redisConnectionFactory, Long.class);
		this.longOperations = longRedisTemplate.opsForValue();
		this.zSetOperations = this.redisOperations.boundZSetOps(this.keys);
	}

	/**
	 * The prefix for all metrics keys.
	 * 
	 * @param prefix the prefix to set for all metrics keys
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
		this.keys = this.prefix + "keys";
	}

	@Override
	public Metric<?> findOne(String metricName) {
		String redisKey = keyFor(metricName);
		String raw = this.redisOperations.opsForValue().get(redisKey);
		if (raw != null) {
			return deserialize(redisKey, raw);
		}
		else {
			return null;
		}
	}

	@Override
	public Iterable<Metric<?>> findAll() {

		// This set is sorted
		Set<String> keys = this.zSetOperations.range(0, -1);
		Iterator<String> keysIt = keys.iterator();

		List<Metric<?>> result = new ArrayList<Metric<?>>(keys.size());
		List<String> values = this.redisOperations.opsForValue().multiGet(keys);
		for (String v : values) {
			result.add(deserialize(keysIt.next(), v));
		}
		return result;

	}

	@Override
	public long count() {
		return this.zSetOperations.size();
	}

	@Override
	public void increment(Delta<?> delta) {
		String name = delta.getName();
		String key = keyFor(name);
		trackMembership(key);
		this.longOperations.increment(key, delta.getValue().longValue());
	}

	@Override
	public void set(Metric<?> value) {
		String raw = serialize(value);
		String name = value.getName();
		String key = keyFor(name);
		trackMembership(key);
		this.redisOperations.opsForValue().set(key, raw);
	}

	@Override
	public void reset(String metricName) {
		String key = keyFor(metricName);
		if (this.zSetOperations.remove(key) == 1) {
			this.redisOperations.delete(key);
		}
	}

	private Metric<?> deserialize(String redisKey, String v) {
		String[] vals = v.split("@");
		Double value = Double.valueOf(vals[0]);
		Date timestamp = vals.length > 1 ? new Date(Long.valueOf(vals[1])) : new Date();
		return new Metric<Double>(nameFor(redisKey), value, timestamp);
	}

	private String serialize(Metric<?> entity) {
		return String.valueOf(entity.getValue()) + "@" + entity.getTimestamp().getTime();
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
