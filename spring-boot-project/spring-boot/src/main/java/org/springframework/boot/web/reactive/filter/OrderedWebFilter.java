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

package org.springframework.boot.web.reactive.filter;

import org.springframework.core.Ordered;
import org.springframework.web.server.WebFilter;

/**
 * An {@link Ordered} {@link org.springframework.web.server.WebFilter}.
 *
 * @author Phillip Webb
 * @since 2.1.0
 */
public interface OrderedWebFilter extends WebFilter, Ordered {

	/**
	 * Filters that wrap the request should be ordered less than or equal to this.
	 */
	int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;

}
