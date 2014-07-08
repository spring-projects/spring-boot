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
import org.springframework.util.Assert;

/**
 * A {@link MetricRepository} implementation for a redis backend. Metric values are stored
 * as zset values plus a regular hash value for the timestamp, both against a key composed
 * of the metric name prefixed with a constant (default "spring.metrics."). If you have
 * multiple metrics repositories all point at the same instance of Redis, it may be useful
 * to change the prefix to be unique (but not if you want them to contribute to the same
 * metrics).
 *
 * @author Dave Syer
 */
public class RedisMetricRepository implements MetricRepository {

	private static final String DEFAULT_METRICS_PREFIX = "spring.metrics.";

	private String prefix = DEFAULT_METRICS_PREFIX;

	private String key = "keys." + DEFAULT_METRICS_PREFIX;

	private BoundZSetOperations<String, String> zSetOperations;

	private final RedisOperations<String, String> redisOperations;

	public RedisMetricRepository(RedisConnectionFactory redisConnectionFactory) {
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		this.redisOperations = RedisUtils.stringTemplate(redisConnectionFactory);
		this.zSetOperations = this.redisOperations.boundZSetOps(this.key);
	}

	/**
	 * The prefix for all metrics keys.
	 * @param prefix the prefix to set for all metrics keys
	 */
	public void setPrefix(String prefix) {
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		this.prefix = prefix;
	}

	/**
	 * The redis key to use to store the index of other keys. The redis store will hold a
	 * zset under this key. Defaults to "keys.spring.metrics". Read operations, especially
	 * {@link #findAll()} and {@link #count()}, will be much more efficient if the key is
	 * unique to the {@link #setPrefix(String) prefix} of this repository.
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
		this.zSetOperations = this.redisOperations.boundZSetOps(this.key);
	}

	@Override
	public Metric<?> findOne(String metricName) {
		String redisKey = keyFor(metricName);
		String raw = this.redisOperations.opsForValue().get(redisKey);
		return deserialize(redisKey, raw, this.zSetOperations.score(redisKey));
	}

	@Override
	public Iterable<Metric<?>> findAll() {

		// This set is sorted
		Set<String> keys = this.zSetOperations.range(0, -1);
		Iterator<String> keysIt = keys.iterator();

		List<Metric<?>> result = new ArrayList<Metric<?>>(keys.size());
		List<String> values = this.redisOperations.opsForValue().multiGet(keys);
		for (String v : values) {
			String key = keysIt.next();
			Metric<?> value = deserialize(key, v, this.zSetOperations.score(key));
			if (value != null) {
				result.add(value);
			}
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
		double value = this.zSetOperations.incrementScore(key, delta.getValue()
				.doubleValue());
		String raw = serialize(new Metric<Double>(name, value, delta.getTimestamp()));
		this.redisOperations.opsForValue().set(key, raw);
	}

	@Override
	public void set(Metric<?> value) {
		String name = value.getName();
		String key = keyFor(name);
		trackMembership(key);
		this.zSetOperations.add(key, value.getValue().doubleValue());
		String raw = serialize(value);
		this.redisOperations.opsForValue().set(key, raw);
	}

	@Override
	public void reset(String metricName) {
		String key = keyFor(metricName);
		if (this.zSetOperations.remove(key) == 1) {
			this.redisOperations.delete(key);
		}
	}

	private Metric<?> deserialize(String redisKey, String v, Double value) {
		if (redisKey == null || v == null || !redisKey.startsWith(this.prefix)) {
			return null;
		}
		Date timestamp = new Date(Long.valueOf(v));
		return new Metric<Double>(nameFor(redisKey), value, timestamp);
	}

	private String serialize(Metric<?> entity) {
		return String.valueOf(entity.getTimestamp().getTime());
	}

	private String keyFor(String name) {
		return this.prefix + name;
	}

	private String nameFor(String redisKey) {
		return redisKey.substring(this.prefix.length());
	}

	private void trackMembership(String redisKey) {
		this.zSetOperations.incrementScore(redisKey, 0.0D);
	}

}
