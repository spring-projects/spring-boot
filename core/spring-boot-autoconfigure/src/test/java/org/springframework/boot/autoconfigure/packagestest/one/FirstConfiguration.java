/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.packagestest.one;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

/**
 * Sample configuration used in {@code AutoConfigurationPackagesTests}.
 *
 * @author Oliver Gierke
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage
public class FirstConfiguration {

}
