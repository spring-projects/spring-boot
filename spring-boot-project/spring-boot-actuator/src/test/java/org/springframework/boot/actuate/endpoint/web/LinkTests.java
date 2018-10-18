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

package org.springframework.boot.actuate.endpoint.web;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Link}.
 *
 * @author Phillip Webb
 */
public class LinkTests {

	@Test
	public void createWhenHrefIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Link(null))
				.withMessageContaining("HREF must not be null");
	}

	@Test
	public void getHrefShouldReturnHref() {
		String href = "http://example.com";
		Link link = new Link(href);
		assertThat(link.getHref()).isEqualTo(href);
	}

	@Test
	public void isTemplatedWhenContainsPlaceholderShouldReturnTrue() {
		String href = "http://example.com/{path}";
		Link link = new Link(href);
		assertThat(link.isTemplated()).isTrue();
	}

	@Test
	public void isTemplatedWhenContainsNoPlaceholderShouldReturnFalse() {
		String href = "http://example.com/path";
		Link link = new Link(href);
		assertThat(link.isTemplated()).isFalse();
	}

}
