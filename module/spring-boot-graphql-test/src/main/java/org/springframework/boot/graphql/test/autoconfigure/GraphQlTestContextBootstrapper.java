/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link GraphQlTest @GraphQlTest}.
 *
 * @author Brian Clozel
 */
class GraphQlTestContextBootstrapper extends TestSliceTestContextBootstrapper<GraphQlTest> {

}
