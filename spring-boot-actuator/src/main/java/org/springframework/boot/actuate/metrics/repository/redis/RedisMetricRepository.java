/*
 * Copyright 2012-2016 the original author or authors.
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

	private static final String DEFAULT_KEY = "keys.spring.metrics";

	private String prefix = DEFAULT_METRICS_PREFIX;

	private String key = DEFAULT_KEY;

	private BoundZSetOperations<String, String> zSetOperations;

	private final RedisOperations<String, String> redisOperations;

	/**
	 * Create a RedisMetricRepository with a default prefix to apply to all metric names.
	 * If multiple repositories share a redis instance they will feed into the same global
	 * metrics.
	 * @param redisConnectionFactory the redis connection factory
	 */
	public RedisMetricRepository(RedisConnectionFactory redisConnectionFactory) {
		this(redisConnectionFactory, null);
	}

	/**
	 * Create a RedisMetricRepository with a prefix to apply to all metric names (ideally
	 * unique to this repository or to a logical repository contributed to by multiple
	 * instances, where they all see the same values). Recommended constructor for general
	 * purpose use.
	 * @param redisConnectionFactory the redis connection factory
	 * @param prefix the prefix to set for all metrics keys
	 */
	public RedisMetricRepository(RedisConnectionFactory redisConnectionFactory,
			String prefix) {
		this(redisConnectionFactory, prefix, null);
	}

	/**
	 * Allows user to set the prefix and key to use to store the index of other keys. The
	 * redis store will hold a zset under the key just so the metric names can be
	 * enumerated. Read operations, especially {@link #findAll()} and {@link #count()},
	 * will only be accurate if the key is unique to the prefix of this repository.
	 * @param redisConnectionFactory the redis connection factory
	 * @param prefix the prefix to set for all metrics keys
	 * @param key the key to set
	 */
	public RedisMetricRepository(RedisConnectionFactory redisConnectionFactory,
			String prefix, String key) {
		if (prefix == null) {
			prefix = DEFAULT_METRICS_PREFIX;
			if (key == null) {
				key = DEFAULT_KEY;
			}
		}
		else if (key == null) {
			key = "keys." + prefix;
		}
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		this.redisOperations = RedisUtils.stringTemplate(redisConnectionFactory);
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		this.prefix = prefix;
		if (key.endsWith(".")) {
			key = key.substring(0, key.length() - 1);
		}
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
		double value = this.zSetOperations.incrementScore(key,
				delta.getValue().doubleValue());
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
