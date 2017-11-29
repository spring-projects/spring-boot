/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.cache;

import java.util.function.Function;

import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInfo;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;

/**
 * {@link OperationMethodInvokerAdvisor} to optionally wrap an {@link OperationInvoker}
 * with a {@link CachingOperationInvoker}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CachingOperationInvokerAdvisor implements OperationMethodInvokerAdvisor {

	private final Function<String, Long> endpointIdTimeToLive;

	public CachingOperationInvokerAdvisor(Function<String, Long> endpointIdTimeToLive) {
		this.endpointIdTimeToLive = endpointIdTimeToLive;
	}

	@Override
	public OperationInvoker apply(String endpointId, OperationMethodInfo methodInfo,
			OperationInvoker invoker) {
		if (methodInfo.getOperationType() == OperationType.READ
				&& methodInfo.getParameters().isEmpty()) {
			Long timeToLive = this.endpointIdTimeToLive.apply(endpointId);
			if (timeToLive != null && timeToLive > 0) {
				return new CachingOperationInvoker(invoker, timeToLive);
			}
		}
		return invoker;
	}

}
