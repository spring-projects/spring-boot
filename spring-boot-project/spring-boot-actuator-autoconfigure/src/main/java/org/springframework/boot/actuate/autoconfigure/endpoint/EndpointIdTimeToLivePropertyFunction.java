/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.time.Duration;
import java.util.function.Function;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

/**
 * Function for use with {@link CachingOperationInvokerAdvisor} that extracts caching
 * time-to-live from a {@link PropertyResolver resolved property}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class EndpointIdTimeToLivePropertyFunction implements Function<EndpointId, Long> {

	private static final Bindable<Duration> DURATION = Bindable.of(Duration.class);

	private final Environment environment;

	/**
	 * Create a new instance with the {@link PropertyResolver} to use.
	 * @param environment the environment
	 */
	EndpointIdTimeToLivePropertyFunction(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Long apply(EndpointId endpointId) {
		String name = String.format("management.endpoint.%s.cache.time-to-live",
				endpointId.toLowerCaseString());
		BindResult<Duration> duration = Binder.get(this.environment).bind(name, DURATION);
		return duration.map(Duration::toMillis).orElse(null);
	}

}
