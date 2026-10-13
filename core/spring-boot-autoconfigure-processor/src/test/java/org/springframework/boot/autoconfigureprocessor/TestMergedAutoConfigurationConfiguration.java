/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Test @AutoConfiguration aliases together with @AutoConfigureBefore
 * and @AutoConfigureAfter.
 *
 * @author Moritz Halbritter
 */
@TestAutoConfigureBefore(value = InputStream.class, name = { "test.before1", "test.before2" })
@TestAutoConfigureAfter(value = OutputStream.class, name = { "test.after1", "test.after2" })
@TestAutoConfiguration(before = ObjectInputStream.class, beforeName = { "test.before3", "test.before4" },
		after = ObjectOutputStream.class, afterName = { "test.after3", "test.after4" })
class TestMergedAutoConfigurationConfiguration {

}
