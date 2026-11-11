/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.PathMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingWebEndpointPathMapper}.
 *
 * @author Stephane Nicoll
 */
class MappingWebEndpointPathMapperTests {

	@Test
	void defaultConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(Collections.emptyMap());
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("test"))).isEqualTo("test");
	}

	@Test
	void userConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(
				Collections.singletonMap("test", "custom"));
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("test")))
			.isEqualTo("custom");
	}

	@Test
	void mixedCaseDefaultConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(Collections.emptyMap());
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("testEndpoint")))
			.isEqualTo("testEndpoint");
	}

	@Test
	void mixedCaseUserConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(
				Collections.singletonMap("test-endpoint", "custom"));
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("testEndpoint")))
			.isEqualTo("custom");
	}

}
