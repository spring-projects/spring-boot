/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Link}.
 *
 * @author Phillip Webb
 */
class LinkTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenHrefIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Link(null))
			.withMessageContaining("'href' must not be null");
	}

	@Test
	void getHrefShouldReturnHref() {
		String href = "https://example.com";
		Link link = new Link(href);
		assertThat(link.getHref()).isEqualTo(href);
	}

	@Test
	void isTemplatedWhenContainsPlaceholderShouldReturnTrue() {
		String href = "https://example.com/{path}";
		Link link = new Link(href);
		assertThat(link.isTemplated()).isTrue();
	}

	@Test
	void isTemplatedWhenContainsNoPlaceholderShouldReturnFalse() {
		String href = "https://example.com/path";
		Link link = new Link(href);
		assertThat(link.isTemplated()).isFalse();
	}

}
