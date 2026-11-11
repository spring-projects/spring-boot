/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.hateoas.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

public interface CustomerRepository {

	List<Customer> findAll();

	@Nullable Customer findOne(Long id);

}
