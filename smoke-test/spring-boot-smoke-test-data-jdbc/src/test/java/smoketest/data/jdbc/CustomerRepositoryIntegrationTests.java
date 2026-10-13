/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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
