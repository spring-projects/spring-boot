/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.web.server.ForwardedRequestHeaders;
import org.springframework.boot.web.server.ForwardedRequestHeaders.Naming;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ServerPropertyAdapters}.
 *
 * @author Phillip Webb
 */
class ServerPropertyAdaptersTests {

	@Nested
	class NativeForwardedRequestHeadersAdapterTests {

		@Test
		void adaptWhenNewAndDeprecatedPropertiesAreSetThrowsException() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.request.header.forwarded.strategy", "web-server");
			environment.setProperty("server.forward-headers-strategy", "none");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> adapt(environment));
		}

		@Test
		void adaptWhenNativeSpecified() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.request.header.forwarded.strategy", "web-server");
			environment.setProperty("server.request.header.forwarded.naming", "x-forwarded");
			environment.setProperty("server.request.header.forwarded.use-prefix", "true");
			assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.X_FORWARDED, true));
		}

		@Test
		void adaptWhenNativeSpecifiedUsingDeprecateProperty() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.forward-headers-strategy", "native");
			assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.X_FORWARDED, false));
		}

		@Test
		void adaptWhenFrameworkAndClouldPlatformUsesForwardHeadersReturnsNull() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.request.header.forwarded.strategy", "framework");
			environment.setProperty("spring.main.cloud-platform", "cloud_foundry");
			assertThat(adapt(environment)).isNull();
		}

		@Test
		void adaptWhenNoneAndClouldPlatformUsesForwardHeadersReturnsNull() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.request.header.forwarded.strategy", "none");
			environment.setProperty("spring.main.cloud-platform", "cloud_foundry");
			assertThat(adapt(environment)).isNull();
		}

		@Test
		void adaptWhenDeprecatedFrameworkAndClouldPlatformUsesForwardHeadersReturnsNull() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.forward-headers-strategy", "framework");
			environment.setProperty("spring.main.cloud-platform", "cloud_foundry");
			assertThat(adapt(environment)).isNull();
		}

		@Test
		void adaptWhenDeprecatedNoneAndClouldPlatformUsesForwardHeadersReturnsNull() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("server.forward-headers-strategy", "none");
			environment.setProperty("spring.main.cloud-platform", "cloud_foundry");
			assertThat(adapt(environment)).isNull();
		}

		@Test
		void adaptWhenNotSpecifiedAndNoEnvironmentReturnsNull() {
			assertThat(adapt(null)).isNull();
		}

		@Test
		void adaptWhenNotSpecifiedAndClouldPlatformUsesForwardHeaders() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("spring.main.cloud-platform", "cloud_foundry");
			assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.X_FORWARDED, false));
		}

		@Test
		void adaptWhenNotSpecifiedAndClouldPlatformDoesNotUseForwardHeaders() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("spring.main.cloud-platform", "none");
			assertThat(adapt(environment)).isNull();
		}

		private @Nullable ForwardedRequestHeaders adapt(@Nullable Environment environment) {
			ServerProperties serverProperties = (environment != null)
					? Binder.get(environment).bindOrCreate("server", ServerProperties.class) : new ServerProperties();
			return ServerPropertyAdapters.webServerForwardedRequestHeaders(environment).adapt(serverProperties);
		}

	}

}
