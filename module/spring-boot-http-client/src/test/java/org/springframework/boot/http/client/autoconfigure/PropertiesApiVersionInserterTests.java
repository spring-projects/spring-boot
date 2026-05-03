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

package org.springframework.boot.http.client.autoconfigure;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.autoconfigure.ApiversionProperties.Insert;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ApiVersionInserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertiesApiVersionInserter}.
 *
 * @author Phillip Webb
 */
class PropertiesApiVersionInserterTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getWhenPropertiesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PropertiesApiVersionInserter.get(null))
			.withMessage("'properties' must not be null");
	}

	@Test
	void getReturnsInserterBasedOnProperties() throws Exception {
		Insert properties = new ApiversionProperties().getInsert();
		properties.setHeader("x-test");
		properties.setQueryParameter("v");
		properties.setPathSegment(1);
		properties.setMediaTypeParameter("mtp");
		ApiVersionInserter inserter = PropertiesApiVersionInserter.get(properties);
		URI uri = new URI("https://example.com/foo/bar");
		assertThat(inserter.insertVersion("123", uri)).hasToString("https://example.com/foo/123/bar?v=123");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		inserter.insertVersion("123", headers);
		assertThat(headers.get("x-test")).containsExactly("123");
		MediaType contentType = headers.getContentType();
		assertThat(contentType).isNotNull();
		assertThat(contentType.getParameters()).containsEntry("mtp", "123");
	}

	@Test
	void getWhenNoPropertiesReturnsEmpty() {
		Insert properties = new ApiversionProperties().getInsert();
		ApiVersionInserter inserter = PropertiesApiVersionInserter.get(properties);
		assertThat(inserter).isEqualTo(PropertiesApiVersionInserter.EMPTY);
	}

}
