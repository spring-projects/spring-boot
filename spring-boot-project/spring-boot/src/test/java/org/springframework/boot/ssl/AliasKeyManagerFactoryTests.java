/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.ssl;

import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AliasKeyManagerFactory}.
 *
 * @author Phillip Webb
 */
class AliasKeyManagerFactoryTests {

	@Test
	void chooseEngineServerAliasReturnsAlias() throws Exception {
		KeyManagerFactory delegate = mock(KeyManagerFactory.class);
		given(delegate.getKeyManagers()).willReturn(new KeyManager[] { mock(X509ExtendedKeyManager.class) });
		AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias",
				KeyManagerFactory.getDefaultAlgorithm());
		factory.init(null, null);
		KeyManager[] keyManagers = factory.getKeyManagers();
		X509ExtendedKeyManager x509KeyManager = (X509ExtendedKeyManager) Arrays.stream(keyManagers)
			.filter(X509ExtendedKeyManager.class::isInstance)
			.findAny()
			.get();
		String chosenAlias = x509KeyManager.chooseEngineServerAlias(null, null, null);
		assertThat(chosenAlias).isEqualTo("test-alias");
	}

}
