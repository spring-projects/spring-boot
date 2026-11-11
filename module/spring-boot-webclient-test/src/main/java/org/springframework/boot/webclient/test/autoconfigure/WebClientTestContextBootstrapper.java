/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link WebClientTest @WebClientTest} support.
 *
 * @author Andy Wilkinson
 */
class WebClientTestContextBootstrapper extends TestSliceTestContextBootstrapper<WebClientTest> {

}
