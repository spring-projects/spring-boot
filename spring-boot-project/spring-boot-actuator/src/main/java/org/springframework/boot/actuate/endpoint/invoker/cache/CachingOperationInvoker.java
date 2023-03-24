/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.invoker.cache;

import java.security.Principal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

/**
 * An {@link OperationInvoker} that caches the response of an operation with a
 * configurable time to live.
 *
 * @author Stephane Nicoll
 * @author Christoph Dreis
 * @author Phillip Webb
 * @since 2.0.0
 */
public class CachingOperationInvoker implements OperationInvoker {

	private static final boolean IS_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Mono", null);

	private static final int CACHE_CLEANUP_THRESHOLD = 40;

	private final OperationInvoker invoker;

	private final long timeToLive;

	private final Map<CacheKey, CachedResponse> cachedResponses;

	/**
	 * Create a new instance with the target {@link OperationInvoker} to use to compute
	 * the response and the time to live for the cache.
	 * @param invoker the {@link OperationInvoker} this instance wraps
	 * @param timeToLive the maximum time in milliseconds that a response can be cached
	 */
	CachingOperationInvoker(OperationInvoker invoker, long timeToLive) {
		Assert.isTrue(timeToLive > 0, "TimeToLive must be strictly positive");
		this.invoker = invoker;
		this.timeToLive = timeToLive;
		this.cachedResponses = new ConcurrentReferenceHashMap<>();
	}

	/**
	 * Return the maximum time in milliseconds that a response can be cached.
	 * @return the time to live of a response
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}

	@Override
	public Object invoke(InvocationContext context) {
		if (hasInput(context)) {
			return this.invoker.invoke(context);
		}
		long accessTime = System.currentTimeMillis();
		if (this.cachedResponses.size() > CACHE_CLEANUP_THRESHOLD) {
			cleanExpiredCachedResponses(accessTime);
		}
		CacheKey cacheKey = getCacheKey(context);
		CachedResponse cached = this.cachedResponses.get(cacheKey);
		if (cached == null || cached.isStale(accessTime, this.timeToLive)) {
			Object response = this.invoker.invoke(context);
			cached = createCachedResponse(response, accessTime);
			this.cachedResponses.put(cacheKey, cached);
		}
		return cached.getResponse();
	}

	private CacheKey getCacheKey(InvocationContext context) {
		ApiVersion contextApiVersion = context.resolveArgument(ApiVersion.class);
		Principal principal = context.resolveArgument(Principal.class);
		WebServerNamespace serverNamespace = context.resolveArgument(WebServerNamespace.class);
		return new CacheKey(contextApiVersion, principal, serverNamespace);
	}

	private void cleanExpiredCachedResponses(long accessTime) {
		try {
			Iterator<Entry<CacheKey, CachedResponse>> iterator = this.cachedResponses.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<CacheKey, CachedResponse> entry = iterator.next();
				if (entry.getValue().isStale(accessTime, this.timeToLive)) {
					iterator.remove();
				}
			}
		}
		catch (Exception ex) {
		}
	}

	private boolean hasInput(InvocationContext context) {
		Map<String, Object> arguments = context.getArguments();
		if (!ObjectUtils.isEmpty(arguments)) {
			return arguments.values().stream().anyMatch(Objects::nonNull);
		}
		return false;
	}

	private CachedResponse createCachedResponse(Object response, long accessTime) {
		if (IS_REACTOR_PRESENT) {
			return new ReactiveCachedResponse(response, accessTime, this.timeToLive);
		}
		return new CachedResponse(response, accessTime);
	}

	static boolean isApplicable(OperationParameters parameters) {
		for (OperationParameter parameter : parameters) {
			if (parameter.isMandatory() && !CacheKey.containsType(parameter.getType())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * A cached response that encapsulates the response itself and the time at which it
	 * was created.
	 */
	static class CachedResponse {

		private final Object response;

		private final long creationTime;

		CachedResponse(Object response, long creationTime) {
			this.response = response;
			this.creationTime = creationTime;
		}

		boolean isStale(long accessTime, long timeToLive) {
			return (accessTime - this.creationTime) >= timeToLive;
		}

		Object getResponse() {
			return this.response;
		}

	}

	/**
	 * {@link CachedResponse} variant used when Reactor is present.
	 */
	static class ReactiveCachedResponse extends CachedResponse {

		ReactiveCachedResponse(Object response, long creationTime, long timeToLive) {
			super(applyCaching(response, timeToLive), creationTime);
		}

		private static Object applyCaching(Object response, long timeToLive) {
			if (response instanceof Mono) {
				return ((Mono<?>) response).cache(Duration.ofMillis(timeToLive));
			}
			if (response instanceof Flux) {
				return ((Flux<?>) response).cache(Duration.ofMillis(timeToLive));
			}
			return response;
		}

	}

	private static final class CacheKey {

		private static final Class<?>[] CACHEABLE_TYPES = new Class<?>[] { ApiVersion.class, SecurityContext.class,
				WebServerNamespace.class };

		private final ApiVersion apiVersion;

		private final Principal principal;

		private final WebServerNamespace serverNamespace;

		private CacheKey(ApiVersion apiVersion, Principal principal, WebServerNamespace serverNamespace) {
			this.principal = principal;
			this.apiVersion = apiVersion;
			this.serverNamespace = serverNamespace;
		}

		static boolean containsType(Class<?> type) {
			return Arrays.stream(CacheKey.CACHEABLE_TYPES).anyMatch((c) -> c.isAssignableFrom(type));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			CacheKey other = (CacheKey) obj;
			return this.apiVersion.equals(other.apiVersion)
					&& ObjectUtils.nullSafeEquals(this.principal, other.principal)
					&& ObjectUtils.nullSafeEquals(this.serverNamespace, other.serverNamespace);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.apiVersion.hashCode();
			result = prime * result + ObjectUtils.nullSafeHashCode(this.principal);
			result = prime * result + ObjectUtils.nullSafeHashCode(this.serverNamespace);
			return result;
		}

	}

}
