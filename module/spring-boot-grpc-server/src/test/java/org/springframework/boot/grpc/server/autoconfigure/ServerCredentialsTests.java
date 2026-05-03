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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.grpc.TlsServerCredentials.ClientAuth;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServerCredentials}.
 *
 * @author Phillip Webb
 */
class ServerCredentialsTests {

	private final TrustManagerFactory insecureTrustManagerFactory = mock();

	private final SslBundles bundles = mock();

	private final TrustManagerFactory bundleTrustManagerFactory = mock();

	private final KeyManagerFactory bundleKeyManagerFactory = mock();

	ServerCredentialsTests() {
		SslBundle bundle = mock();
		SslManagerBundle managers = mock();
		given(this.bundles.getBundle("test")).willReturn(bundle);
		given(bundle.getManagers()).willReturn(managers);
		given(managers.getTrustManagerFactory()).willReturn(this.bundleTrustManagerFactory);
		given(managers.getKeyManagerFactory()).willReturn(this.bundleKeyManagerFactory);
	}

	@Test
	void getWhenNotEnabledAndNoBundleReturnsNullManagers() {
		ServerCredentials credentials = get((properties) -> {
		});
		assertThat(credentials.keyManagerFactory()).isNull();
		assertThat(credentials.trustManagerFactory()).isNull();
		assertThat(credentials.clientAuth()).isEqualTo(ClientAuth.NONE);
	}

	@Test
	void getWhenDisabledReturnsNullManagers() {
		ServerCredentials credentials = get((properties) -> {
			properties.put("spring.grpc.server.ssl.enabled", "false");
			properties.put("spring.grpc.server.ssl.client-auth", "require");
		});
		assertThat(credentials.keyManagerFactory()).isNull();
		assertThat(credentials.trustManagerFactory()).isNull();
		assertThat(credentials.clientAuth()).isEqualTo(ClientAuth.REQUIRE);
	}

	@Test
	void getWhenEnabledTrueAndNoBundleNameThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> get((properties) -> {
			properties.put("spring.grpc.server.ssl.enabled", "true");
			properties.put("spring.grpc.server.ssl.client-auth", "require");
		})).withMessage("SSL bundle-name is requested when 'spring.grpc.server.ssl.enabled' is true");
	}

	@Test
	void getWhenHasBundleName() {
		ServerCredentials credentials = get((properties) -> {
			properties.put("spring.grpc.server.ssl.bundle", "test");
			properties.put("spring.grpc.server.ssl.client-auth", "require");
		});
		assertThat(credentials.keyManagerFactory()).isEqualTo(this.bundleKeyManagerFactory);
		assertThat(credentials.trustManagerFactory()).isEqualTo(this.bundleTrustManagerFactory);
		assertThat(credentials.clientAuth()).isEqualTo(ClientAuth.REQUIRE);
	}

	@Test
	void getWhenHasBundleNameAndEnabled() {
		ServerCredentials credentials = get((properties) -> {
			properties.put("spring.grpc.server.ssl.enabled", "true");
			properties.put("spring.grpc.server.ssl.bundle", "test");
			properties.put("spring.grpc.server.ssl.client-auth", "require");
		});
		assertThat(credentials.keyManagerFactory()).isEqualTo(this.bundleKeyManagerFactory);
		assertThat(credentials.trustManagerFactory()).isEqualTo(this.bundleTrustManagerFactory);
		assertThat(credentials.clientAuth()).isEqualTo(ClientAuth.REQUIRE);
	}

	@Test
	void getWhenHasBundleNameAndSecureFalse() {
		ServerCredentials credentials = get((properties) -> {
			properties.put("spring.grpc.server.ssl.enabled", "true");
			properties.put("spring.grpc.server.ssl.bundle", "test");
			properties.put("spring.grpc.server.ssl.secure", "false");
		});
		assertThat(credentials.keyManagerFactory()).isEqualTo(this.bundleKeyManagerFactory);
		assertThat(credentials.trustManagerFactory()).isEqualTo(this.insecureTrustManagerFactory);
		assertThat(credentials.clientAuth()).isEqualTo(ClientAuth.NONE);
	}

	private ServerCredentials get(Consumer<Map<String, String>> properties) {
		Map<String, String> map = new HashMap<>();
		properties.accept(map);
		Ssl ssl = new Binder(new MapConfigurationPropertySource(map))
			.bind("spring.grpc.server", GrpcServerProperties.class)
			.orElseGet(GrpcServerProperties::new)
			.getSsl();
		return ServerCredentials.get(ssl, this.bundles, this.insecureTrustManagerFactory);
	}

}
