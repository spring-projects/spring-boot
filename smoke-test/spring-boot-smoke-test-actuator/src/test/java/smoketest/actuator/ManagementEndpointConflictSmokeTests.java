/*
 * Copyright 2012-present the original author or authors.
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
