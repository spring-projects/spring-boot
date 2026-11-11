/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.domain;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Data JPA tests for {@link User}.
 *
 * @author Phillip Webb
 */
@DataJpaTest
class UserEntityTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber("00000000000000000");

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenUsernameIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User(null, VIN))
			.withMessage("'username' must not be empty");
	}

	@Test
	void createWhenUsernameIsEmptyShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User("", VIN))
			.withMessage("'username' must not be empty");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenVinIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User("sboot", null))
			.withMessage("'vin' must not be null");
	}

	@Test
	void saveShouldPersistData() {
		User user = this.entityManager.persistFlushFind(new User("sboot", VIN));
		assertThat(user.getUsername()).isEqualTo("sboot");
		assertThat(user.getVin()).isEqualTo(VIN);
	}

}
