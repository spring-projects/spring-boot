/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Map;
import java.util.Objects;

import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An {@link OperationInvoker} that caches the response of an operation with a
 * configurable time to live.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CachingOperationInvoker implements OperationInvoker {

	private final OperationInvoker invoker;

	private final long timeToLive;

	private volatile CachedResponse cachedResponse;

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
		CachedResponse cached = this.cachedResponse;
		if (cached == null || cached.isStale(accessTime, this.timeToLive)) {
			Object response = this.invoker.invoke(context);
			this.cachedResponse = new CachedResponse(response, accessTime);
			return response;
		}
		return cached.getResponse();
	}

	private boolean hasInput(InvocationContext context) {
		if (context.getSecurityContext().getPrincipal() != null) {
			return true;
		}
		Map<String, Object> arguments = context.getArguments();
		if (!ObjectUtils.isEmpty(arguments)) {
			return arguments.values().stream().anyMatch(Objects::nonNull);
		}
		return false;
	}

	/**
	 * Apply caching configuration when appropriate to the given invoker.
	 * @param invoker the invoker to wrap
	 * @param timeToLive the maximum time in milliseconds that a response can be cached
	 * @return a caching version of the invoker or the original instance if caching is not
	 * required
	 */
	public static OperationInvoker apply(OperationInvoker invoker, long timeToLive) {
		if (timeToLive > 0) {
			return new CachingOperationInvoker(invoker, timeToLive);
		}
		return invoker;
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

		public boolean isStale(long accessTime, long timeToLive) {
			return (accessTime - this.creationTime) >= timeToLive;
		}

		public Object getResponse() {
			return this.response;
		}

	}

}
