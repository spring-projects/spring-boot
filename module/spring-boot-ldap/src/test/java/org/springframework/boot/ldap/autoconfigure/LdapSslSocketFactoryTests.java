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

package org.springframework.boot.ldap.autoconfigure;

import java.io.InputStream;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link LdapSslSocketFactory}.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class LdapSslSocketFactoryTests {

	@AfterEach
	void clearSslBundle() {
		LdapSslSocketFactory.setSslBundle(null);
	}

	@Test
	void shouldFailWhenNoSslBundleHasBeenSet() {
		assertThatIllegalStateException().isThrownBy(LdapSslSocketFactory::getDefault)
			.withMessage("No SSL bundle has been set");
	}

	@Test
	void shouldUseSocketFactoryFromSslBundle() {
		SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
		LdapSslSocketFactory.setSslBundle(sslBundle(socketFactory));
		assertThat(LdapSslSocketFactory.getDefault()).extracting("delegate").isSameAs(socketFactory);
	}

	@Test
	void shouldDelegateCreationOfUnconnectedSocket() throws Exception {
		SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
		Socket socket = mock(Socket.class);
		given(socketFactory.createSocket()).willReturn(socket);
		LdapSslSocketFactory.setSslBundle(sslBundle(socketFactory));
		assertThat(LdapSslSocketFactory.getDefault().createSocket()).isSameAs(socket);
	}

	@Test
	void shouldDelegateCreationOfSocketWithConsumedInput() throws Exception {
		SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
		Socket socket = mock(Socket.class);
		Socket wrapped = mock(Socket.class);
		InputStream consumed = InputStream.nullInputStream();
		given(socketFactory.createSocket(socket, consumed, true)).willReturn(wrapped);
		LdapSslSocketFactory.setSslBundle(sslBundle(socketFactory));
		assertThat(((SSLSocketFactory) LdapSslSocketFactory.getDefault()).createSocket(socket, consumed, true))
			.isSameAs(wrapped);
	}

	@Test
	void shouldWarnWhenReplacingBundleWithADifferentOne(CapturedOutput output) {
		LdapSslSocketFactory.setSslBundle(sslBundle(mock(SSLSocketFactory.class)));
		LdapSslSocketFactory.setSslBundle(sslBundle(mock(SSLSocketFactory.class)));
		assertThat(output).contains("A different SSL bundle has already been set for LDAP");
	}

	@Test
	void shouldNotWarnWhenSettingTheSameBundleAgain(CapturedOutput output) {
		SslBundle sslBundle = sslBundle(mock(SSLSocketFactory.class));
		LdapSslSocketFactory.setSslBundle(sslBundle);
		LdapSslSocketFactory.setSslBundle(sslBundle);
		assertThat(output).doesNotContain("A different SSL bundle has already been set for LDAP");
	}

	@Test
	void shouldCreateSslContextForEachInvocationToPickUpReloadedMaterial() {
		SslBundle sslBundle = sslBundle(mock(SSLSocketFactory.class));
		LdapSslSocketFactory.setSslBundle(sslBundle);
		LdapSslSocketFactory.getDefault();
		LdapSslSocketFactory.getDefault();
		then(sslBundle).should(times(2)).createSslContext();
	}

	private SslBundle sslBundle(SSLSocketFactory socketFactory) {
		SSLContext sslContext = mock(SSLContext.class);
		given(sslContext.getSocketFactory()).willReturn(socketFactory);
		SslBundle sslBundle = mock(SslBundle.class);
		given(sslBundle.createSslContext()).willReturn(sslContext);
		return sslBundle;
	}

}
