/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestEndpoint;

/**
 * An endpoint with an upper camel case id.
 *
 * @author Stephane Nicoll
 */
@TestEndpoint(id = "PascalCase")
public class CamelCaseEndpoint {

}
