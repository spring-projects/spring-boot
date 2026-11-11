/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CityRepository extends ReactiveCrudRepository<City, Long> {

}
