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
import org.springframework.http.ResponseEntity;

/**
 * Integration tests for adding Health probes additional paths.
 *
 * @author Ivo Smid
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoint.health.probes.enabled=true",
				"management.endpoint.health.probes.add-additional-paths=true" })
class ManagementPortAvailabilityProbesDefaultPathTests extends AbstractManagementPortAvailabilityProbesTests {

	@Test
	void testLivez() {
		ResponseEntity<String> entity = callHttpGetServer("/livez");
		assertUp(entity);
	}

	@Test
	void testReadyz() {
		ResponseEntity<String> entity = callHttpGetServer("/readyz");
		assertUp(entity);
	}

}
