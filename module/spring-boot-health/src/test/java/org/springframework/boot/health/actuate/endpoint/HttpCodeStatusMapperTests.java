/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpCodeStatusMapper}.
 *
 * @author Phillip Webb
 */
class HttpCodeStatusMapperTests {

	@Test
	void createWhenMappingsAreNullUsesDefaultMappings() {
		HttpCodeStatusMapper mapper = HttpCodeStatusMapper.of(null);
		assertThat(mapper.getStatusCode(Status.UNKNOWN)).isEqualTo(WebEndpointResponse.STATUS_OK);
		assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(WebEndpointResponse.STATUS_OK);
		assertThat(mapper.getStatusCode(Status.DOWN)).isEqualTo(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE))
			.isEqualTo(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
	}

	@Test
	void getStatusCodeReturnsMappedStatus() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("up", 123);
		map.put("down", 456);
		HttpCodeStatusMapper mapper = HttpCodeStatusMapper.of(map);
		assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(123);
		assertThat(mapper.getStatusCode(Status.DOWN)).isEqualTo(456);
		assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE)).isEqualTo(200);
	}

	@Test
	void getStatusCodeWhenMappingsAreNotUniformReturnsMappedStatus() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("out-of-service", 123);
		HttpCodeStatusMapper mapper = HttpCodeStatusMapper.of(map);
		assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE)).isEqualTo(123);
	}

}
