/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.flyway;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long> {

}
