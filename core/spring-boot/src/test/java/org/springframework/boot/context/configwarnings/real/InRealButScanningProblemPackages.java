/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.configwarnings.real;

import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageConfiguration;
import org.springframework.boot.context.configwarnings.orgspring.InOrgSpringPackageConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = { InDefaultPackageConfiguration.class, InOrgSpringPackageConfiguration.class })
public class InRealButScanningProblemPackages {

}
