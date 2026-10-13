/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginLookup}.
 *
 * @author Phillip Webb
 */
class OriginLookupTests {

	@Test
	void getOriginWhenSourceIsNullShouldReturnNull() {
		assertThat(OriginLookup.getOrigin(null, "foo")).isNull();
	}

	@Test
	void getOriginWhenSourceIsNotLookupShouldReturnLookupOrigin() {
		Object source = new Object();
		assertThat(OriginLookup.getOrigin(source, "foo")).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void getOriginWhenSourceIsLookupShouldReturnLookupOrigin() {
		OriginLookup<String> source = mock(OriginLookup.class);
		Origin origin = MockOrigin.of("bar");
		given(source.getOrigin("foo")).willReturn(origin);
		assertThat(OriginLookup.getOrigin(source, "foo")).isEqualTo(origin);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getOriginWhenSourceLookupThrowsAndErrorShouldReturnNull() {
		OriginLookup<String> source = mock(OriginLookup.class);
		willThrow(RuntimeException.class).given(source).getOrigin("foo");
		assertThat(OriginLookup.getOrigin(source, "foo")).isNull();
	}

}
