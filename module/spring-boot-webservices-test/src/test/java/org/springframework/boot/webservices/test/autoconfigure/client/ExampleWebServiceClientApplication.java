/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Example {@link SpringBootApplication @SpringBootApplication} used with
 * {@link WebServiceClientTest @WebServiceClientTest} tests.
 *
 * @author Dmytro Nosan
 */
@SpringBootApplication
@Import(WebServiceMarshallerConfiguration.class)
public class ExampleWebServiceClientApplication {

}
