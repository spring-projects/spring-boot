/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataNeo4jTest @DataNeo4jTest} support.
 *
 * @author Artsiom Yudovin
 */
class DataNeo4jTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataNeo4jTest> {

}
