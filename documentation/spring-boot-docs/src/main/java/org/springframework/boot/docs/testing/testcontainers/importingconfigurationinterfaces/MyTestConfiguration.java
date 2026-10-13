/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.testcontainers.importingconfigurationinterfaces;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(MyContainers.class)
class MyTestConfiguration {

}
