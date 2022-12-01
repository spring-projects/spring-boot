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

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock Security Provider for testing purposes only (e.g. SslServerCustomizerTests class)
 *
 * @author Cyril Dangerville
 */
public class MockKeyStoreSpi extends KeyStoreSpi {

	private static final KeyPairGenerator KEYGEN;

	static {
		try {
			KEYGEN = KeyPairGenerator.getInstance("RSA");
			KEYGEN.initialize(2048);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	private final Map<String, KeyPair> aliases = new HashMap<>();

	@Override
	public Key engineGetKey(String alias, char[] password) {
		final KeyPair keyPair = this.aliases.get(alias);
		return (keyPair != null) ? keyPair.getPrivate() : null;
	}

	@Override
	public Certificate[] engineGetCertificateChain(String alias) {
		return new Certificate[0];
	}

	@Override
	public Certificate engineGetCertificate(String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Date engineGetCreationDate(String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void engineSetCertificateEntry(String alias, Certificate cert) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void engineDeleteEntry(String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<String> engineAliases() {
		return Collections.enumeration(this.aliases.keySet());
	}

	@Override
	public boolean engineContainsAlias(String alias) {
		// contains any required alias, for testing purposes
		// Add alias to aliases list on the fly
		this.aliases.put(alias, KEYGEN.generateKeyPair());
		return true;
	}

	@Override
	public int engineSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean engineIsKeyEntry(String alias) {
		// Handle all keystore entries as key entries
		return this.aliases.containsKey(alias);
	}

	@Override
	public boolean engineIsCertificateEntry(String alias) {
		return false;
	}

	@Override
	public String engineGetCertificateAlias(Certificate cert) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void engineStore(OutputStream stream, char[] password) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void engineLoad(InputStream stream, char[] password) {
		// Nothing to do, this is a mock keystore implementation, for testing only.
	}

}
