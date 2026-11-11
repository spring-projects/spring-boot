/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoker.cache;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;

/**
 * {@link OperationInvokerAdvisor} to optionally provide result caching support.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CachingOperationInvokerAdvisor implements OperationInvokerAdvisor {

	private final Function<EndpointId, @Nullable Long> endpointIdTimeToLive;

	public CachingOperationInvokerAdvisor(Function<EndpointId, @Nullable Long> endpointIdTimeToLive) {
		this.endpointIdTimeToLive = endpointIdTimeToLive;
	}

	@Override
	public OperationInvoker apply(EndpointId endpointId, OperationType operationType, OperationParameters parameters,
			OperationInvoker invoker) {
		if (operationType == OperationType.READ && CachingOperationInvoker.isApplicable(parameters)) {
			Long timeToLive = this.endpointIdTimeToLive.apply(endpointId);
			if (timeToLive != null && timeToLive > 0) {
				return new CachingOperationInvoker(invoker, timeToLive);
			}
		}
		return invoker;
	}

}
