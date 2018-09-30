/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.test.domain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private TestEntityManager entityManager;

	@Test
	public void createWhenUsernameIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Username must not be empty");
		new User(null, VIN);
	}

	@Test
	public void createWhenUsernameIsEmptyShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Username must not be empty");
		new User("", VIN);
	}

	@Test
	public void createWhenVinIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("VIN must not be null");
		new User("sboot", null);
	}

	@Test
	public void saveShouldPersistData() {
		User user = this.entityManager.persistFlushFind(new User("sboot", VIN));
		assertThat(user.getUsername()).isEqualTo("sboot");
		assertThat(user.getVin()).isEqualTo(VIN);
	}

}
