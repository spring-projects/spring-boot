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

package org.springframework.boot.grpc.server.health;

import java.util.LinkedHashMap;
import java.util.Map;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatusMapper}.
 *
 * @author Phillip Webb
 */
class StatusMapperTests {

	@Test
	void createWhenMappingsAreNullUsesDefaultMappings() {
		StatusMapper mapper = StatusMapper.of(null);
		assertThat(mapper.getServingStatus(Status.UNKNOWN)).isEqualTo(ServingStatus.UNKNOWN);
		assertThat(mapper.getServingStatus(Status.UP)).isEqualTo(ServingStatus.SERVING);
		assertThat(mapper.getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.NOT_SERVING);
		assertThat(mapper.getServingStatus(Status.OUT_OF_SERVICE)).isEqualTo(ServingStatus.NOT_SERVING);
	}

	@Test
	void getStatusCodeReturnsMappedStatus() {
		Map<String, ServingStatus> map = new LinkedHashMap<>();
		map.put("up", ServingStatus.UNRECOGNIZED);
		map.put("down", ServingStatus.UNKNOWN);
		StatusMapper mapper = StatusMapper.of(map);
		assertThat(mapper.getServingStatus(Status.UP)).isEqualTo(ServingStatus.UNRECOGNIZED);
		assertThat(mapper.getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.UNKNOWN);
		assertThat(mapper.getServingStatus(Status.OUT_OF_SERVICE)).isEqualTo(ServingStatus.SERVING);
	}

	@Test
	void getStatusCodeWhenMappingsAreNotUniformReturnsMappedStatus() {
		Map<String, ServingStatus> map = new LinkedHashMap<>();
		map.put("out-of-service", ServingStatus.SERVING);
		StatusMapper mapper = StatusMapper.of(map);
		assertThat(mapper.getServingStatus(Status.OUT_OF_SERVICE)).isEqualTo(ServingStatus.SERVING);
	}

}
