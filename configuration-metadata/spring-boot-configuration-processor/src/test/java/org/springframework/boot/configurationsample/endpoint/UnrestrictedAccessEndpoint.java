/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestEndpoint;

/**
 * An endpoint with unrestricted access unless configured explicitly.
 *
 * @author Andy Wilkinson
 */
@TestEndpoint(id = "unrestrictedaccess")
public class UnrestrictedAccessEndpoint {

}
