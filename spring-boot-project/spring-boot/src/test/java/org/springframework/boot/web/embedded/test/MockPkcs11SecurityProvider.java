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

package org.springframework.boot.web.embedded.test;

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
