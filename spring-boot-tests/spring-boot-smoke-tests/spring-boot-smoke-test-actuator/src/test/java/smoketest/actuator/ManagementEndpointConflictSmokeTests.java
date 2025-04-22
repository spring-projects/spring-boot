/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies that an exception is thrown when management and server endpoint paths
 * conflict.
 *
 * @author Yongjun Hong
 */
class ManagementEndpointConflictSmokeTests {

	@Test
	void shouldThrowExceptionWhenManagementAndServerPathsConflict() {
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(() -> SpringApplication.run(SampleActuatorApplication.class,
					"--management.endpoints.web.base-path=/", "--management.endpoints.web.path-mapping.health=/"))
			.withMessageContaining("Management base path and the 'health' actuator endpoint are both mapped to '/'");
	}

}
