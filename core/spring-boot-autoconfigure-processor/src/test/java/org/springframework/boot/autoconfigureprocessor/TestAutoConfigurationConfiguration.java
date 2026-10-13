/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test @AutoConfiguration aliases for @AutoConfigureBefore and @AutoConfigureAfter.
 *
 * @author Moritz Halbritter
 */
@TestAutoConfiguration(before = InputStream.class, beforeName = { "test.before1", "test.before2" },
		after = OutputStream.class, afterName = { "test.after1", "test.after2" })
class TestAutoConfigurationConfiguration {

}
