/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link RestClientTest @RestClientTest} support.
 *
 * @author Artsiom Yudovin
 */
class RestClientTestContextBootstrapper extends TestSliceTestContextBootstrapper<RestClientTest> {

}
