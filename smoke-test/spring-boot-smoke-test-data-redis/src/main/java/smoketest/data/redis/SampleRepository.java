/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.redis;

import org.springframework.data.repository.CrudRepository;

interface SampleRepository extends CrudRepository<PersonHash, String> {

}
