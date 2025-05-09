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
import org.mockito.ArgumentMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link org.springframework.boot.ssl.AliasKeyManagerFactory}.
 *
 * @author Phillip Webb
 * @author Stéphane Gobancé
 */
class AliasKeyManagerFactoryTests {

	@Test
	void chooseEngineServerAliasReturnsTestAliasMatchingSupportedDelegateAliasAndAlgorithm() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = delegateSupportedAlias[1];
		final String testAlgorithm = delegateSupportedAlgorithm;
		final String expectedAlias = testAlias;

		KeyManagerFactory delegate = mockServerKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseEngineServerAlias(testAlgorithm, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseEngineServerAliasReturnsNullWhenTestAliasIsUnknown() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = "unknown-alias";
		final String testAlgorithm = delegateSupportedAlgorithm;
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockServerKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseEngineServerAlias(testAlgorithm, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseEngineServerAliasReturnsNullWhenAlgorithmDoesNotMatch() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[1];
		final String testAlias = delegateSupportedAlias[0];
		final String testAlgorithm = "other-alg";
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockServerKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseEngineServerAlias(testAlgorithm, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseServerAliasReturnsTestAliasMatchingSupportedDelegateAliasAndAlgorithm() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = delegateSupportedAlias[1];
		final String testAlgorithm = delegateSupportedAlgorithm;
		final String expectedAlias = testAlias;

		KeyManagerFactory delegate = mockServerKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseServerAlias(testAlgorithm, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseServerAliasReturnsNullWhenTestAliasIsUnknown() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = "unknown-alias";
		final String testAlgorithm = delegateSupportedAlgorithm;
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockServerKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseServerAlias(testAlgorithm, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseServerAliasReturnsNullWhenAlgorithmDoesNotMatch() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[1];
		final String testAlias = delegateSupportedAlias[0];
		final String testAlgorithm = "other-alg";
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockServerKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseServerAlias(testAlgorithm, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseEngineClientAliasReturnsTestAliasMatchingSupportedDelegateAliasAndAlgorithm() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = delegateSupportedAlias[1];
		final String[] testAlgorithms = new String[] { delegateSupportedAlgorithm };
		final String expectedAlias = testAlias;

		KeyManagerFactory delegate = mockClientKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseEngineClientAlias(testAlgorithms, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseEngineClientAliasReturnsNullWhenTestAliasIsUnknown() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = "unknown-alias";
		final String[] testAlgorithms = new String[] { delegateSupportedAlgorithm };
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockClientKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseEngineClientAlias(testAlgorithms, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseEngineClientAliasReturnsNullWhenAlgorithmDoesNotMatch() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[1];
		final String testAlias = delegateSupportedAlias[0];
		final String[] testAlgorithms = new String[] { "other-alg" };
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockClientKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseEngineClientAlias(testAlgorithms, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseClientAliasReturnsTestAliasMatchingSupportedDelegateAliasAndAlgorithm() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = delegateSupportedAlias[1];
		final String[] testAlgorithms = new String[] { delegateSupportedAlgorithm };
		final String expectedAlias = testAlias;

		KeyManagerFactory delegate = mockClientKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseClientAlias(testAlgorithms, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseClientAliasReturnsNullWhenTestAliasIsUnknown() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[0];
		final String testAlias = "unknown-alias";
		final String[] testAlgorithms = new String[] { delegateSupportedAlgorithm };
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockClientKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseClientAlias(testAlgorithms, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	@Test
	void chooseClientAliasReturnsNullWhenAlgorithmDoesNotMatch() throws Exception {

		final String delegateSupportedAlgorithm = "supported-alg";
		final String[] delegateSupportedAlias = new String[] { "alias0", "alias1", "alias2" };
		final String delegateChosenAlias = delegateSupportedAlias[1];
		final String testAlias = delegateSupportedAlias[0];
		final String[] testAlgorithms = new String[] { "other-alg" };
		final String expectedAlias = null;

		KeyManagerFactory delegate = mockClientKeyManagerFactory(delegateSupportedAlgorithm, delegateSupportedAlias,
				delegateChosenAlias);
		AliasKeyManagerFactory factory = createAliasKeyManagerFactory(testAlias, delegate);
		X509ExtendedKeyManager x509KeyManager = getX509ExtendedKeyManager(factory);

		String chosenAlias = x509KeyManager.chooseClientAlias(testAlgorithms, null, null);
		assertThat(chosenAlias).isEqualTo(expectedAlias);
	}

	private static AliasKeyManagerFactory createAliasKeyManagerFactory(String alias, KeyManagerFactory delegate)
			throws Exception {
		AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, alias, delegate.getAlgorithm());
		factory.init(null, null);
		return factory;
	}

	private static X509ExtendedKeyManager getX509ExtendedKeyManager(KeyManagerFactory factory) {
		return Arrays.stream(factory.getKeyManagers())
			.filter(X509ExtendedKeyManager.class::isInstance)
			.map(X509ExtendedKeyManager.class::cast)
			.findAny()
			.get();
	}

	private static KeyManagerFactory mockServerKeyManagerFactory(String algorithm, String[] serverAliases,
			String serverChosenAlias) {

		return mockKeyManagerFactory(algorithm, serverAliases, serverChosenAlias, null, null);
	}

	private static KeyManagerFactory mockClientKeyManagerFactory(String algorithm, String[] clientAliases,
			String clientChosenAlias) {

		return mockKeyManagerFactory(algorithm, null, null, clientAliases, clientChosenAlias);
	}

	private static KeyManagerFactory mockKeyManagerFactory(String algorithm, String[] serverAliases,
			String serverChosenAlias, String[] clientAliases, String clientChosenAlias) {

		KeyManagerFactory delegate = mock(KeyManagerFactory.class);
		X509ExtendedKeyManager x509KeyManagerMock = mock(X509ExtendedKeyManager.class);
		given(delegate.getAlgorithm()).willReturn(algorithm);
		given(delegate.getKeyManagers()).willReturn(new KeyManager[] { x509KeyManagerMock });
		given(x509KeyManagerMock.getServerAliases(eq(algorithm), any())).willReturn(serverAliases);
		given(x509KeyManagerMock.chooseServerAlias(eq(algorithm), any(), any())).willReturn(serverChosenAlias);
		given(x509KeyManagerMock.chooseEngineServerAlias(eq(algorithm), any(), any())).willReturn(serverChosenAlias);
		given(x509KeyManagerMock.getClientAliases(eq(algorithm), any())).willReturn(clientAliases);
		given(x509KeyManagerMock.chooseClientAlias(argThat(arrayContains(algorithm)), any(), any()))
			.willReturn(clientChosenAlias);
		given(x509KeyManagerMock.chooseEngineClientAlias(argThat(arrayContains(algorithm)), any(), any()))
			.willReturn(clientChosenAlias);
		return delegate;
	}

	private static ArgumentMatcher<String[]> arrayContains(String expected) {
		return (array) -> array != null && Arrays.asList(array).contains(expected);
	}

}
