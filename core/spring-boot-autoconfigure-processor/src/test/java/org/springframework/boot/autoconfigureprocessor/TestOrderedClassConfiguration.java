/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.io.ObjectInputStream;
import java.io.OutputStream;

/**
 * Test configuration with an annotated class.
 *
 * @author Phillip Webb
 */
@TestAutoConfigureBefore(name = { "test.before1", "test.before2" })
@TestAutoConfigureAfter(ObjectInputStream.class)
@TestConditionalOnClass(name = "java.io.InputStream", value = OutputStream.class)
@TestAutoConfigureOrder(123)
public class TestOrderedClassConfiguration {

}
