/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.servlet.filter;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the ordering of various {@link OrderedFilter} implementations.
 *
 * @author Andy Wilkinson
 */
class OrderedFilterOrderingTests {

	@Test
	void ordering() {
		OrderedCharacterEncodingFilter characterEncoding = new OrderedCharacterEncodingFilter();
		OrderedFormContentFilter formContent = new OrderedFormContentFilter();
		OrderedHiddenHttpMethodFilter hiddenHttpMethod = new OrderedHiddenHttpMethodFilter();
		OrderedRequestContextFilter requestContext = new OrderedRequestContextFilter();
		List<OrderedFilter> filters = new ArrayList<>(
				List.of(characterEncoding, formContent, hiddenHttpMethod, requestContext));
		AnnotationAwareOrderComparator.sort(filters);
		assertThat(filters).containsExactly(characterEncoding, hiddenHttpMethod, formContent, requestContext);
	}

	@Test
	void requestContextOrderingIsCloseToRequestWrapperFilterMaxOrder() {
		assertThat(new OrderedRequestContextFilter().getOrder())
			.isCloseTo(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER, Assertions.within(105));
	}

}
