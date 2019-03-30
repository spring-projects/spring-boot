/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.test.domain;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Data JPA tests for {@link User}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserEntityTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber(
			"00000000000000000");

	@Autowired
	private TestEntityManager entityManager;

	@Test
	public void createWhenUsernameIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User(null, VIN))
				.withMessage("Username must not be empty");
	}

	@Test
	public void createWhenUsernameIsEmptyShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User("", VIN))
				.withMessage("Username must not be empty");
	}

	@Test
	public void createWhenVinIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User("sboot", null))
				.withMessage("VIN must not be null");
	}

	@Test
	public void saveShouldPersistData() {
		User user = this.entityManager.persistFlushFind(new User("sboot", VIN));
		assertThat(user.getUsername()).isEqualTo("sboot");
		assertThat(user.getVin()).isEqualTo(VIN);
	}

}
