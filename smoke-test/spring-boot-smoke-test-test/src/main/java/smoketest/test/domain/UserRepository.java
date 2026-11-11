/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.domain;

import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.Repository;

/**
 * Domain repository for {@link User}.
 *
 * @author Phillip Webb
 */
public interface UserRepository extends Repository<User, Long> {

	@Nullable User findByUsername(String username);

}
