/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link AliasKeyManagerFactory}.
 *
 * @author Phillip Webb
 */
class AliasKeyManagerFactoryTests {
	private static final String SERVER_ALIAS = "server-alias";

	private static final String CLIENT_ALIAS = "client-alias";

	@Test
	void chooseEngineServerAliasReturnsAlias() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, SERVER_ALIAS, CLIENT_ALIAS);
		String chosenAlias = keyManager.chooseEngineServerAlias(null, null, null);

		assertThat(chosenAlias).isEqualTo(SERVER_ALIAS);
		verifyNoInteractions(delegate);
	}

	@Test
	void chooseServerAliasReturnsAlias() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(mock(X509ExtendedKeyManager.class), SERVER_ALIAS, CLIENT_ALIAS);
		String chosenAlias = keyManager.chooseServerAlias(null, null, null);

		assertThat(chosenAlias).isEqualTo(SERVER_ALIAS);
		verifyNoInteractions(delegate);
	}

	@Test
	void chooseEngineClientAliasReturnsAlias() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, SERVER_ALIAS, CLIENT_ALIAS);
		String chosenAlias = keyManager.chooseEngineClientAlias(null, null, null);

		assertThat(chosenAlias).isEqualTo(CLIENT_ALIAS);
		verifyNoInteractions(delegate);
	}

	@Test
	void chooseClientAliasReturnsAlias() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, SERVER_ALIAS, CLIENT_ALIAS);

		String chosenAlias = keyManager.chooseClientAlias(null, null, null);
		assertThat(chosenAlias).isEqualTo(CLIENT_ALIAS);
		verifyNoInteractions(delegate);
	}

	@Test
	void chooseEngineServerAliasDelegatesWhenServerAliasIsNull() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, null, null);

		given(delegate.chooseEngineServerAlias(any(), any(), any())).willReturn(SERVER_ALIAS);

		String alias = keyManager.chooseEngineServerAlias(null, null, null);
		verify(delegate).chooseEngineServerAlias(null, null, null);
		assertThat(alias).isEqualTo(SERVER_ALIAS);
	}

	@Test
	void chooseServerAliasDelegatesWhenServerAliasIsNull() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, null, null);

		given(delegate.chooseServerAlias(any(), any(), any())).willReturn(SERVER_ALIAS);

		String alias = keyManager.chooseServerAlias(null, null, null);
		verify(delegate).chooseServerAlias(null, null, null);
		assertThat(alias).isEqualTo(SERVER_ALIAS);
	}


	@Test
	void chooseEngineClientAliasDelegatesWhenClientAliasIsNull() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, null, null);

		given(delegate.chooseEngineClientAlias(any(), any(), any())).willReturn(CLIENT_ALIAS);

		String alias = keyManager.chooseEngineClientAlias(null, null, null);
		verify(delegate).chooseEngineClientAlias(null, null, null);
		assertThat(alias).isEqualTo(CLIENT_ALIAS);
	}

	@Test
	void chooseClientAliasDelegatesWhenClientAliasIsNull() throws Exception {
		X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
		X509ExtendedKeyManager keyManager = createKeyManager(delegate, null, null);

		given(delegate.chooseClientAlias(any(), any(), any())).willReturn(CLIENT_ALIAS);

		String alias = keyManager.chooseClientAlias(null, null, null);
		verify(delegate).chooseClientAlias(null, null, null);
		assertThat(alias).isEqualTo(CLIENT_ALIAS);
	}

	private X509ExtendedKeyManager createKeyManager(X509ExtendedKeyManager keyManager, @Nullable String serverAlias, @Nullable String clientAlias) throws Exception {
		KeyManagerFactory delegate = mock(KeyManagerFactory.class);
		given(delegate.getKeyManagers()).willReturn(new KeyManager[] { keyManager });

		AliasKeyManagerFactory factory =
				new AliasKeyManagerFactory(delegate, serverAlias, clientAlias, KeyManagerFactory.getDefaultAlgorithm());
		factory.init(null, null);

		return (X509ExtendedKeyManager) Arrays.stream(factory.getKeyManagers())
				.filter(X509ExtendedKeyManager.class::isInstance)
				.findFirst()
				.orElseThrow();
	}
}
