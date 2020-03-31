/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.httpclient;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.httpclient.RemoteEnvironmentDockerHttpClientConnection.EnvironmentAccessor;
import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteEnvironmentDockerHttpClientConnection}.
 *
 * @author Scott Frederick
 */
class RemoteEnvironmentDockerHttpClientConnectionTests {

	private EnvironmentAccessor environment;

	private RemoteEnvironmentDockerHttpClientConnection connection;

	private SslContextFactory sslContextFactory;

	@BeforeEach
	void setUp() {
		this.environment = mock(EnvironmentAccessor.class);
		this.sslContextFactory = mock(SslContextFactory.class);
		this.connection = new RemoteEnvironmentDockerHttpClientConnection(this.environment, this.sslContextFactory);
	}

	@Test
	void notAcceptedWhenDockerHostNotSet() {
		assertThat(this.connection.accept()).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> this.connection.getHttpHost());
		assertThatIllegalStateException().isThrownBy(() -> this.connection.getHttpClient());
	}

	@Test
	void acceptedWhenDockerHostIsSet() {
		given(this.environment.getProperty("DOCKER_HOST")).willReturn("tcp://192.168.1.2:2376");
		assertThat(this.connection.accept()).isTrue();
	}

	@Test
	void invalidTlsConfigurationThrowsException() {
		given(this.environment.getProperty("DOCKER_HOST")).willReturn("tcp://192.168.1.2:2376");
		given(this.environment.getProperty("DOCKER_TLS_VERIFY")).willReturn("1");
		assertThatIllegalArgumentException().isThrownBy(() -> this.connection.accept())
				.withMessageContaining("DOCKER_CERT_PATH");
	}

	@Test
	void hostProtocolIsHttpWhenNotSecure() {
		given(this.environment.getProperty("DOCKER_HOST")).willReturn("tcp://192.168.1.2:2376");
		assertThat(this.connection.accept()).isTrue();
		HttpHost host = this.connection.getHttpHost();
		assertThat(host).isNotNull();
		assertThat(host.getSchemeName()).isEqualTo("http");
		assertThat(host.getHostName()).isEqualTo("192.168.1.2");
		assertThat(host.getPort()).isEqualTo(2376);
	}

	@Test
	void hostProtocolIsHttpsWhenSecure() throws NoSuchAlgorithmException {
		given(this.environment.getProperty("DOCKER_HOST")).willReturn("tcp://192.168.1.2:2376");
		given(this.environment.getProperty("DOCKER_TLS_VERIFY")).willReturn("1");
		given(this.environment.getProperty("DOCKER_CERT_PATH")).willReturn("/test-cert-path");
		given(this.sslContextFactory.forPath("/test-cert-path")).willReturn(SSLContext.getDefault());
		assertThat(this.connection.accept()).isTrue();
		HttpHost host = this.connection.getHttpHost();
		assertThat(host).isNotNull();
		assertThat(host.getSchemeName()).isEqualTo("https");
		assertThat(host.getHostName()).isEqualTo("192.168.1.2");
		assertThat(host.getPort()).isEqualTo(2376);
	}

}
