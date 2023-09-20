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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.assertj.core.util.TriFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteHttpClientTransport}
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class RemoteHttpClientTransportTests {

	@Test
	void createIfPossibleWhenDockerHostIsNotSetReturnsNull() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(null);
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		assertThat(transport).isNull();
	}

	@Test
	void createIfPossibleWhenDockerHostIsDefaultReturnsNull() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration.forAddress(null));
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		assertThat(transport).isNull();
	}

	@Test
	void createIfPossibleWhenDockerHostIsFileReturnsNull() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(DockerHostConfiguration.forAddress("unix:///var/run/socket.sock"));
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		assertThat(transport).isNull();
	}

	@Test
	void createIfPossibleWhenDockerHostIsAddressReturnsTransport() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376"));
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		assertThat(transport).isNotNull();
	}

	@Test
	void createIfPossibleWhenNoTlsVerifyUsesHttp() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376"));
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		assertThat(transport.getHost()).satisfies(hostOf("http", "192.168.1.2", 2376));
	}

	@Test
	void createIfPossibleWhenTlsVerifyUsesHttps() throws Exception {
		SslContextFactory sslContextFactory = mock(SslContextFactory.class);
		given(sslContextFactory.forDirectory("/test-cert-path")).willReturn(SSLContext.getDefault());
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376", true, "/test-cert-path"));
		RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost, sslContextFactory);
		assertThat(transport.getHost()).satisfies(hostOf("https", "192.168.1.2", 2376));
	}

	@Test
	void createIfPossibleWhenTlsVerifyWithMissingCertPathThrowsException() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376", true, null));
		assertThatIllegalArgumentException().isThrownBy(() -> RemoteHttpClientTransport.createIfPossible(dockerHost))
			.withMessageContaining("Docker host TLS verification requires trust material");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void createIfPossibleWhenSocketTimeoutIsDefault(boolean secured) throws NoSuchAlgorithmException {

		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration
			.forAddress("tcp://192.168.1.2:2376", secured, (secured) ? "/test-cert-path" : null));
		RemoteHttpClientTransport transport;
		if (secured) {
			SslContextFactory sslContextFactory = mock(SslContextFactory.class);
			given(sslContextFactory.forDirectory("/test-cert-path")).willReturn(SSLContext.getDefault());
			transport = RemoteHttpClientTransport.createIfPossible(dockerHost, sslContextFactory);
		}
		else {
			transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		}

		Resolver<?, ?> resolver = findTransportSocketConfigResolver(transport);
		Object socketConfigObj = resolver.resolve(null);
		assertThat(socketConfigObj).describedAs("Null socketConfig is for DEFAULT socketConfig during connection")
			.isNull();

	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void createIfPossibleWhenSocketTimeoutIsConfigured(boolean secured) throws NoSuchAlgorithmException {

		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration
			.forAddress("tcp://192.168.1.2:2376", secured, (secured) ? "/test-cert-path" : null)
			.withSocketTimeout(123456));
		RemoteHttpClientTransport transport;
		if (secured) {
			SslContextFactory sslContextFactory = mock(SslContextFactory.class);
			given(sslContextFactory.forDirectory("/test-cert-path")).willReturn(SSLContext.getDefault());
			transport = RemoteHttpClientTransport.createIfPossible(dockerHost, sslContextFactory);
		}
		else {
			transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
		}

		Resolver<?, ?> resolver = findTransportSocketConfigResolver(transport);
		Object socketConfigObj = resolver.resolve(null);
		assertThat(socketConfigObj).isInstanceOfSatisfying(SocketConfig.class,
				(socketConfig) -> assertThat(socketConfig.getSoTimeout()).hasToString("123456 SECONDS"));
	}

	private static Resolver<?, ?> findTransportSocketConfigResolver(RemoteHttpClientTransport transport) {
		TriFunction<Class<?>, String, Object, Object> getField = (aClass, fieldName, object) -> {
			Field field = ReflectionUtils.findField(aClass, fieldName);
			ReflectionUtils.makeAccessible(field);
			return ReflectionUtils.getField(field, object);
		};
		Object clientObj = getField.apply(RemoteHttpClientTransport.class, "client", transport);
		Object connManagerObj = getField.apply(clientObj.getClass(), "connManager", clientObj);
		Object socketConfigResolverObj = getField.apply(connManagerObj.getClass(), "socketConfigResolver",
				connManagerObj);

		if (socketConfigResolverObj instanceof Resolver<?, ?>) {
			return (Resolver<?, ?>) socketConfigResolverObj;
		}
		else {
			return fail("Invalid %s type for ", Resolver.class, socketConfigResolverObj);
		}
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
