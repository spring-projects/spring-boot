/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.Collection;

import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.core.env.Environment;

/**
 * {@link EndpointFilter} that will filter endpoints based on {@code include} and
 * {@code exclude} patterns.
 *
 * @param <E> the endpoint type
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 2.2.7 in favor of {@link IncludeExcludeEndpointFilter}
 */
@Deprecated
public class ExposeExcludePropertyEndpointFilter<E extends ExposableEndpoint<?>>
		extends IncludeExcludeEndpointFilter<E> {

	public ExposeExcludePropertyEndpointFilter(Class<E> endpointType, Environment environment, String prefix,
			String... exposeDefaults) {
		super(endpointType, environment, prefix, exposeDefaults);
	}

	public ExposeExcludePropertyEndpointFilter(Class<E> endpointType, Collection<String> include,
			Collection<String> exclude, String... exposeDefaults) {
		super(endpointType, include, exclude, exposeDefaults);
	}

}
