/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.data.cassandra;

import org.springframework.boot.data.cassandra.test.autoconfigure.DataCassandraTest;
import org.springframework.data.cassandra.repository.CassandraRepository;

/**
 * Example repository used with {@link DataCassandraTest @DataCassandraTest} tests.
 *
 * @author Artsiom Yudovin
 */
interface ExampleRepository extends CassandraRepository<ExampleEntity, String> {

}
