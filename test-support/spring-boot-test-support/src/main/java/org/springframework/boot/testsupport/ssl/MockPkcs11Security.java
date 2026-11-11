/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.ssl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * JUnit {@link ExtendWith @ExtendWith} annotation to support
 * {@link MockPkcs11SecurityProvider}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockPkcs11SecurityProviderExtension.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockPkcs11Security {

}
