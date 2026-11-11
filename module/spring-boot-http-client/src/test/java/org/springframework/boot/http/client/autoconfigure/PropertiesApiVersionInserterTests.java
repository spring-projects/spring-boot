/*
 * Copyright 2012-present the original author or authors.
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
