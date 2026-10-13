/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link JsonTest @JsonTest} support.
 *
 * @author Artsiom Yudovin
 */
class JsonTestContextBootstrapper extends TestSliceTestContextBootstrapper<JsonTest> {

}
