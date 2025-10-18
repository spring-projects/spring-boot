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

package org.springframework.boot.test.http.server;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BaseUrlProviders}.
 *
 * @author Phillip Webb
 */
class BaseUrlProvidersTests {

	@Test
	void getBaseUrlWhenNoProvidedBaseUrlReturnsNull() {
		assertThat(new BaseUrlProviders(Collections.emptyList()).getBaseUrl()).isNull();
	}

	@Test
	void getBaseUrlWithFallbackWhenNoProvidedBaseUrlReturnsFallback() {
		BaseUrl fallback = BaseUrl.of("https://example.com");
		assertThat(new BaseUrlProviders(Collections.emptyList()).getBaseUrl(fallback)).isSameAs(fallback);
	}

	@Test
	void getBaseUrlReturnsFirstProvidedBaseUrl() {
		BaseUrlProvider p1 = mock();
		BaseUrlProvider p2 = mock();
		BaseUrl baseUrl = BaseUrl.of("https://example.com");
		given(p1.getBaseUrl()).willReturn(baseUrl);
		assertThat(new BaseUrlProviders(List.of(p1, p2)).getBaseUrl()).isSameAs(baseUrl);
		then(p2).shouldHaveNoInteractions();
	}

}
