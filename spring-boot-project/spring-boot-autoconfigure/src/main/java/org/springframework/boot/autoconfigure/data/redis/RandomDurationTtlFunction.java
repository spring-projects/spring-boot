package org.springframework.boot.autoconfigure.data.redis;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.lang.Nullable;
import org.springframework.data.redis.cache.RedisCacheWriter.TtlFunction;

/**
 * {@link TtlFunction} implementation returning the given, predetermined {@link Duration} used for per cache entry
 * {@literal time-to-live (TTL) expiration}.
 *
 * @author liudaac
 * @see java.time.Duration
 * @see org.springframework.data.redis.cache.RedisCacheWriter.TtlFunction
 * @since 3.2.1
 */
public record RandomDurationTtlFunction(Duration duration, Duration randomOffset) implements TtlFunction {

	@Override
	public Duration getTimeToLive(Object key, Object value) {
		// TODO Auto-generated method stub
		long expiration = duration.toMillis();
		if (shouldExpireWithin(randomOffset)) {
			expiration += ThreadLocalRandom.current().nextLong(randomOffset.toMillis());
		}
		return Duration.ofMillis(expiration);
	}
	
	private static boolean shouldExpireWithin(@Nullable Duration ttl) {
		return ttl != null && !ttl.isZero() && !ttl.isNegative();
	}
}
