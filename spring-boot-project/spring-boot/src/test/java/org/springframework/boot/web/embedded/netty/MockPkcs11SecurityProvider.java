/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.netty;

import java.security.KeyStoreSpi;
import java.security.Provider;

/**
 * Mock PKCS#11 Security Provider for testing purposes only (e.g. SslServerCustomizerTests
 * class)
 *
 * @author Cyril Dangerville
 */
public class MockPkcs11SecurityProvider extends Provider {

	private static final String DEFAULT_PROVIDER_NAME = "Mock-PKCS11";

	private static final String VERSION = "0.1";

	private static final String DESCRIPTION = "Mock PKCS11 Provider";

	/**
	 * Create Security Provider named {@value #DEFAULT_PROVIDER_NAME}, version
	 * {@value #VERSION} and providing PKCS11 KeyStores with {@link MockKeyStoreSpi} as
	 * {@link KeyStoreSpi} implementation.
	 */
	public MockPkcs11SecurityProvider() {
		super(DEFAULT_PROVIDER_NAME, VERSION, DESCRIPTION);

		putService(new Service(this, "KeyStore", "PKCS11",
				"org.springframework.boot.web.embedded.netty.MockKeyStoreSpi", null, null));
	}

}
