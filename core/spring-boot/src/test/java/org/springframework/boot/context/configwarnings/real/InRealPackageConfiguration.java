/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.configwarnings.real;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan
public class InRealPackageConfiguration {

}
