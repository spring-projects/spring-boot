/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.ssl;

import java.security.Security;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@link Extension} to support {@link MockPkcs11SecurityProvider}.
 *
 * @author Phillip Webb
 * @see MockPkcs11Security
 */
class MockPkcs11SecurityProviderExtension implements BeforeAllCallback, AfterAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		Security.addProvider(MockPkcs11SecurityProvider.INSTANCE);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		Security.removeProvider(MockPkcs11SecurityProvider.NAME);
	}

}
