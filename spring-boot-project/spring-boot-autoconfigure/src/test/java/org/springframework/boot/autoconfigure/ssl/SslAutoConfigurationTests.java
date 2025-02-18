/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslAutoConfiguration}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SslAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class));

	@Test
	void sslBundlesCreatedWithNoConfiguration() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(SslBundleRegistry.class));
	}

	@Test
	void sslBundlesCreatedWithCertificates() {
		List<String> propertyValues = new ArrayList<>();
		String location = "classpath:org/springframework/boot/autoconfigure/ssl/";
		propertyValues.add("spring.ssl.bundle.pem.first.key.alias=alias1");
		propertyValues.add("spring.ssl.bundle.pem.first.key.password=secret1");
		propertyValues.add("spring.ssl.bundle.pem.first.keystore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.first.keystore.private-key=" + location + "rsa-key.pem");
		propertyValues.add("spring.ssl.bundle.pem.first.keystore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.first.truststore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.first.truststore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.first.truststore.private-key=" + location + "rsa-key.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.key.alias=alias2");
		propertyValues.add("spring.ssl.bundle.pem.second.key.password=secret2");
		propertyValues.add("spring.ssl.bundle.pem.second.keystore.certificate=" + location + "ed25519-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.keystore.private-key=" + location + "ed25519-key.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.keystore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.second.truststore.certificate=" + location + "ed25519-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.truststore.private-key=" + location + "ed25519-key.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.truststore.type=PKCS12");
		this.contextRunner.withPropertyValues(propertyValues.toArray(String[]::new)).run((context) -> {
			assertThat(context).hasSingleBean(SslBundles.class);
			SslBundles bundles = context.getBean(SslBundles.class);
			SslBundle first = bundles.getBundle("first");
			assertThat(first).isNotNull();
			assertThat(first.getStores()).isNotNull();
			assertThat(first.getManagers()).isNotNull();
			assertThat(first.getKey().getAlias()).isEqualTo("alias1");
			assertThat(first.getKey().getPassword()).isEqualTo("secret1");
			assertThat(first.getStores().getKeyStore().getType()).isEqualTo("PKCS12");
			assertThat(first.getStores().getTrustStore().getType()).isEqualTo("PKCS12");
			SslBundle second = bundles.getBundle("second");
			assertThat(second).isNotNull();
			assertThat(second.getStores()).isNotNull();
			assertThat(second.getManagers()).isNotNull();
			assertThat(second.getKey().getAlias()).isEqualTo("alias2");
			assertThat(second.getKey().getPassword()).isEqualTo("secret2");
			assertThat(second.getStores().getKeyStore().getType()).isEqualTo("PKCS12");
			assertThat(second.getStores().getTrustStore().getType()).isEqualTo("PKCS12");
		});
	}

	@Test
	void sslBundlesCreatedWithCustomSslBundle() {
		List<String> propertyValues = new ArrayList<>();
		String location = "classpath:org/springframework/boot/autoconfigure/ssl/";
		propertyValues.add("custom.ssl.key.alias=alias1");
		propertyValues.add("custom.ssl.key.password=secret1");
		propertyValues.add("custom.ssl.keystore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("custom.ssl.keystore.keystore.private-key=" + location + "rsa-key.pem");
		propertyValues.add("custom.ssl.truststore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("custom.ssl.keystore.type=PKCS12");
		propertyValues.add("custom.ssl.truststore.type=PKCS12");
		this.contextRunner.withUserConfiguration(CustomSslBundleConfiguration.class)
			.withPropertyValues(propertyValues.toArray(String[]::new))
			.run((context) -> {
				assertThat(context).hasSingleBean(SslBundles.class);
				SslBundles bundles = context.getBean(SslBundles.class);
				SslBundle bundle = bundles.getBundle("custom");
				assertThat(bundle).isNotNull();
				assertThat(bundle.getStores()).isNotNull();
				assertThat(bundle.getManagers()).isNotNull();
				assertThat(bundle.getKey().getAlias()).isEqualTo("alias1");
				assertThat(bundle.getKey().getPassword()).isEqualTo("secret1");
				assertThat(bundle.getStores().getKeyStore().getType()).isEqualTo("PKCS12");
				assertThat(bundle.getStores().getTrustStore().getType()).isEqualTo("PKCS12");
			});
	}

	@Test
	void sslBundleWithoutClassPathPrefix() {
		List<String> propertyValues = new ArrayList<>();
		String location = "src/test/resources/org/springframework/boot/autoconfigure/ssl/";
		propertyValues.add("spring.ssl.bundle.pem.test.key.alias=alias1");
		propertyValues.add("spring.ssl.bundle.pem.test.key.password=secret1");
		propertyValues.add("spring.ssl.bundle.pem.test.keystore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.test.keystore.keystore.private-key=" + location + "rsa-key.pem");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.certificate=" + location + "rsa-cert.pem");
		this.contextRunner.withPropertyValues(propertyValues.toArray(String[]::new)).run((context) -> {
			assertThat(context).hasSingleBean(SslBundles.class);
			SslBundles bundles = context.getBean(SslBundles.class);
			SslBundle bundle = bundles.getBundle("test");
			assertThat(bundle.getStores().getKeyStore().getCertificate("alias1")).isNotNull();
			assertThat(bundle.getStores().getTrustStore().getCertificate("ssl")).isNotNull();
		});
	}

	@Configuration
	@EnableConfigurationProperties(CustomSslProperties.class)
	public static class CustomSslBundleConfiguration {

		@Bean
		public SslBundleRegistrar customSslBundlesRegistrar(CustomSslProperties properties) {
			return new CustomSslBundlesRegistrar(properties);
		}

	}

	@ConfigurationProperties("custom.ssl")
	static class CustomSslProperties extends PemSslBundleProperties {

	}

	static class CustomSslBundlesRegistrar implements SslBundleRegistrar {

		private final CustomSslProperties properties;

		CustomSslBundlesRegistrar(CustomSslProperties properties) {
			this.properties = properties;
		}

		@Override
		public void registerBundles(SslBundleRegistry registry) {
			registry.registerBundle("custom", PropertiesSslBundle.get(this.properties));
		}

	}

}
