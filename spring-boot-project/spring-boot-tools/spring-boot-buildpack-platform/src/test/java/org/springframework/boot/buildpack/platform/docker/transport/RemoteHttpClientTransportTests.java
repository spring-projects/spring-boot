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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteHttpClientTransport}
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class RemoteHttpClientTransportTests {

	private Map<String, String> environment = new LinkedHashMap<>();

	@Test
	void createIfPossibleWhenDockerHostIsNotSetReturnsNull() {
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(this.environment::get);
		assertThat(transport).isNull();
	}

	@Test
	void createIfPossibleWhenDockerHostIsSetReturnsTransport() {
		this.environment.put("DOCKER_HOST", "tcp://192.168.1.2:2376");
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(this.environment::get);
		assertThat(transport).isNotNull();
	}

	@Test
	void createIfPossibleWhenTlsVerifyWithMissingCertPathThrowsException() {
		this.environment.put("DOCKER_HOST", "tcp://192.168.1.2:2376");
		this.environment.put("DOCKER_TLS_VERIFY", "1");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RemoteHttpClientTransport.createIfPossible(this.environment::get))
				.withMessageContaining("DOCKER_CERT_PATH");
	}

	@Test
	void createIfPossibleWhenNoTlsVerifyUsesHttp() {
		this.environment.put("DOCKER_HOST", "tcp://192.168.1.2:2376");
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(this.environment::get);
		assertThat(transport.getHost()).satisfies(hostOf("http", "192.168.1.2", 2376));
	}

	@Test
	void createIfPossibleWhenTlsVerifyUsesHttps() throws Exception {
		this.environment.put("DOCKER_HOST", "tcp://192.168.1.2:2376");
		this.environment.put("DOCKER_TLS_VERIFY", "1");
		this.environment.put("DOCKER_CERT_PATH", "/test-cert-path");
		SslContextFactory sslContextFactory = mock(SslContextFactory.class);
		given(sslContextFactory.forDirectory("/test-cert-path")).willReturn(SSLContext.getDefault());
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(this.environment::get,
				sslContextFactory);
		assertThat(transport.getHost()).satisfies(hostOf("https", "192.168.1.2", 2376));
	}

	private Consumer<HttpHost> hostOf(String scheme, String hostName, int port) {
		return (host) -> {
			assertThat(host).isNotNull();
			assertThat(host.getSchemeName()).isEqualTo(scheme);
			assertThat(host.getHostName()).isEqualTo(hostName);
			assertThat(host.getPort()).isEqualTo(port);
		};
	}

}
