/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.ssl;

import java.security.Provider;

/**
 * Mock PKCS#11 Security Provider for testing purposes.
 *
 * @author Cyril Dangerville
 */
public class MockPkcs11SecurityProvider extends Provider {

	/**
	 * The name of the mock provider.
	 */
	public static final String NAME = "Mock-PKCS11";

	static final MockPkcs11SecurityProvider INSTANCE = new MockPkcs11SecurityProvider();

	MockPkcs11SecurityProvider() {
		super(NAME, "0.1", "Mock PKCS11 Provider");
		putService(new Service(this, "KeyStore", "PKCS11", MockKeyStoreSpi.class.getName(), null, null));
	}

}
