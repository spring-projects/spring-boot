/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.test.autoconfigure;

import org.springframework.data.repository.Repository;

/**
 * Example repository used with {@link DataJpaTest @DataJpaTest} tests.
 *
 * @author Phillip Webb
 */
interface ExampleRepository extends Repository<ExampleEntity, Long> {

	ExampleEntity findByReference(String reference);

}
