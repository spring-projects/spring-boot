/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.filter;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
			.isCloseTo(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER, within(105));
	}

}
