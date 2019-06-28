/*
 * Copyright 2012-2019 the original author or authors.
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
package smoketest.data.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CustomerRepository}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureTestDatabase
class CustomerRepositoryIntegrationTests {

	@Autowired
	private CustomerRepository repository;

	@Test
	void findAllCustomers() {
		assertThat(this.repository.findAll()).hasSize(2);
	}

	@Test
	void findByNameWithMatch() {
		assertThat(this.repository.findByName("joan")).hasSize(1);
	}

	@Test
	void findByNameWithNoMatch() {
		assertThat(this.repository.findByName("hugh")).isEmpty();
	}

}
