/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Example {@link SpringBootApplication @SpringBootApplication} used with
 * {@link WebClientTest @WebClientTest} tests.
 *
 * @author Phillip Webb
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ExampleWebClientApplication {

}
