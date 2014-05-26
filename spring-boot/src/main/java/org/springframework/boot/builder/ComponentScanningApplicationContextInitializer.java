/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

/**
 * Spring project-level components scanner. Resolves base package from the system property, so end-user doesn't have to
 * create custom wiring code.
 */
public class ComponentScanningApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String BASE_PACKAGE_PROPERTY_KEY = "spring.boot.basepackage";

    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanningApplicationContextInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        LOG.debug("Initializing Spring Boot component scanner...");
        String basePackage = System.getProperty(BASE_PACKAGE_PROPERTY_KEY);
        if (basePackage != null) {
            LOG.debug("Found base package definition: {}={}", BASE_PACKAGE_PROPERTY_KEY, basePackage);
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) applicationContext, true);
            scanner.setResourceLoader(applicationContext);
            scanner.scan(basePackage);
        } else {
            LOG.debug("No base package definition ({}) found.", BASE_PACKAGE_PROPERTY_KEY);
        }
    }

}