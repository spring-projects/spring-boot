/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestAccess;
import org.springframework.boot.configurationsample.TestEndpoint;

/**
 * An endpoint with read-only access unless configured explicitly.
 *
 * @author Andy Wilkinson
 */
@TestEndpoint(id = "readonlyaccess", defaultAccess = TestAccess.READ_ONLY)
public class ReadOnlyAccessEndpoint {

}
