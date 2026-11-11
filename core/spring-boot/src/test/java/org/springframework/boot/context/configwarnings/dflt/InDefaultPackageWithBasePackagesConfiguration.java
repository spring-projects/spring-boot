/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.configwarnings.dflt;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "org.springframework.boot.context.configwarnings.nested")
public class InDefaultPackageWithBasePackagesConfiguration {

}
