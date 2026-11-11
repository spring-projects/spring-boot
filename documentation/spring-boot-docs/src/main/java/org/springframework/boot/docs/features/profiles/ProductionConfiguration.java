/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.profiles;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("production")
public class ProductionConfiguration {

	// ...

}
