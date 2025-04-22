package smoketest.actuator;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that an exception is thrown when management and server endpoint paths
 * conflict.
 *
 * @author Yongjun Hong
 */
class ManagementEndpointConflictSmokeTest {

	@Test
	void shouldThrowExceptionWhenManagementAndServerPathsConflict() {
		assertThatThrownBy(() -> {
			SpringApplication.run(SampleActuatorApplication.class, "--management.endpoints.web.base-path=/",
					"--management.endpoints.web.path-mapping.health=/");
		}).isInstanceOf(BeanCreationException.class)
			.hasMessageContaining("Management endpoints and endpoint path are both mapped to '/'");
	}

}