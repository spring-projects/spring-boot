/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.actuator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for adding Health probes additional paths.
 *
 * @author Ivo Smid
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoint.health.probes.enabled=true",
				"management.endpoint.health.probes.add-additional-paths=false",
				"management.endpoint.health.group.liveness.additional-path=server:/live-custom",
				"management.endpoint.health.group.readiness.additional-path=server:/ready-custom" })
class ManagementPortAvailabilityProbesCustomPathWithoutFlagEnabledTests
		extends AbstractManagementPortAvailabilityProbesTests {

	@Test
	void testCustomLivez() {
		ResponseEntity<String> entity = callHttpGetServer("/live-custom");
		assertUp(entity);
	}

	@Test
	void testDefaultLivezNotFound() {
		ResponseEntity<String> entity = callHttpGetServer("/livez");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void testCustomReadyz() {
		ResponseEntity<String> entity = callHttpGetServer("/ready-custom");
		assertUp(entity);
	}

	@Test
	void testDefaultReadyzNotFound() {
		ResponseEntity<String> entity = callHttpGetServer("/readyz");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

}
