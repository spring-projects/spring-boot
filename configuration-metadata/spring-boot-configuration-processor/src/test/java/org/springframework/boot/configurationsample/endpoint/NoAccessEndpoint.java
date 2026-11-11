/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestAccess;
import org.springframework.boot.configurationsample.TestEndpoint;

/**
 * An endpoint with no permitted access unless configured explicitly.
 *
 * @author Andy Wilkinson
 */
@TestEndpoint(id = "noaccess", defaultAccess = TestAccess.NONE)
public class NoAccessEndpoint {

}
