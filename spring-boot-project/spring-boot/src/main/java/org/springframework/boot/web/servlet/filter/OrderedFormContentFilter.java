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

package org.springframework.boot.web.servlet.filter;

import org.springframework.core.Ordered;
import org.springframework.web.filter.FormContentFilter;

/**
 * {@link FormContentFilter} that also implements {@link Ordered}.
 *
 * @author Joao Pedro Evangelista
 * @author Brian Clozel
 * @since 2.1.0
 */
public class OrderedFormContentFilter extends FormContentFilter implements OrderedFilter {

	/**
	 * Higher order to ensure the filter is applied before Spring Security.
	 */
	public static final int DEFAULT_ORDER = REQUEST_WRAPPER_FILTER_MAX_ORDER - 9900;

	private int order = DEFAULT_ORDER;

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order for this filter.
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
