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
		propertyValues.add("spring.ssl.bundle.pem.first.key.alias=alias1");
		propertyValues.add("spring.ssl.bundle.pem.first.key.password=secret1");
		propertyValues.add("spring.ssl.bundle.pem.first.keystore.certificate=cert1.pem");
		propertyValues.add("spring.ssl.bundle.pem.first.keystore.private-key=key1.pem");
		propertyValues.add("spring.ssl.bundle.pem.first.keystore.type=JKS");
		propertyValues.add("spring.ssl.bundle.pem.first.truststore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.second.key.alias=alias2");
		propertyValues.add("spring.ssl.bundle.pem.second.key.password=secret2");
		propertyValues.add("spring.ssl.bundle.pem.second.keystore.certificate=cert2.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.keystore.private-key=key2.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.keystore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.second.truststore.certificate=ca.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.truststore.private-key=ca-key.pem");
		propertyValues.add("spring.ssl.bundle.pem.second.truststore.type=JKS");
		this.contextRunner.withPropertyValues(propertyValues.toArray(String[]::new)).run((context) -> {
			assertThat(context).hasSingleBean(SslBundles.class);
			SslBundles bundles = context.getBean(SslBundles.class);
			SslBundle first = bundles.getBundle("first");
			assertThat(first).isNotNull();
			assertThat(first.getStores()).isNotNull();
			assertThat(first.getManagers()).isNotNull();
			assertThat(first.getKey().getAlias()).isEqualTo("alias1");
			assertThat(first.getKey().getPassword()).isEqualTo("secret1");
			assertThat(first.getStores()).extracting("keyStoreDetails").extracting("type").isEqualTo("JKS");
			assertThat(first.getStores()).extracting("trustStoreDetails").extracting("type").isEqualTo("PKCS12");
			SslBundle second = bundles.getBundle("second");
			assertThat(second).isNotNull();
			assertThat(second.getStores()).isNotNull();
			assertThat(second.getManagers()).isNotNull();
			assertThat(second.getKey().getAlias()).isEqualTo("alias2");
			assertThat(second.getKey().getPassword()).isEqualTo("secret2");
			assertThat(second.getStores()).extracting("keyStoreDetails").extracting("type").isEqualTo("PKCS12");
			assertThat(second.getStores()).extracting("trustStoreDetails").extracting("type").isEqualTo("JKS");
		});
	}

	@Test
	void sslBundlesCreatedWithCustomSslBundle() {
		List<String> propertyValues = new ArrayList<>();
		propertyValues.add("custom.ssl.key.alias=alias1");
		propertyValues.add("custom.ssl.key.password=secret1");
		propertyValues.add("custom.ssl.keystore.type=JKS");
		propertyValues.add("custom.ssl.truststore.type=PKCS12");
		this.contextRunner.withUserConfiguration(CustomSslBundleConfiguration.class)
			.withPropertyValues(propertyValues.toArray(String[]::new))
			.run((context) -> {
				assertThat(context).hasSingleBean(SslBundles.class);
				SslBundles bundles = context.getBean(SslBundles.class);
				SslBundle first = bundles.getBundle("custom");
				assertThat(first).isNotNull();
				assertThat(first.getStores()).isNotNull();
				assertThat(first.getManagers()).isNotNull();
				assertThat(first.getKey().getAlias()).isEqualTo("alias1");
				assertThat(first.getKey().getPassword()).isEqualTo("secret1");
				assertThat(first.getStores()).extracting("keyStoreDetails").extracting("type").isEqualTo("JKS");
				assertThat(first.getStores()).extracting("trustStoreDetails").extracting("type").isEqualTo("PKCS12");
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
