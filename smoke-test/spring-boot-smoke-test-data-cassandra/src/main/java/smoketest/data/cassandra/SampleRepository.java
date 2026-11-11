/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;

interface SampleRepository extends CassandraRepository<SampleEntity, String> {

}
