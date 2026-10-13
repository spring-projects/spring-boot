/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.io.OutputStream;

/**
 * Test configuration with an annotated method.
 *
 * @author Madhura Bhave
 */
public class TestMethodConfiguration {

	@TestConditionalOnClass(name = "java.io.InputStream", value = OutputStream.class)
	public Object method() {
		return null;
	}

}
