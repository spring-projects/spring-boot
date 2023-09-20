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

package org.springframework.boot.autoconfigure.security.saml2;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Saml2RelyingPartyProperties}.
 *
 * @author Madhura Bhave
 */
class Saml2RelyingPartyPropertiesTests {

	private final Saml2RelyingPartyProperties properties = new Saml2RelyingPartyProperties();

	@Test
	void customizeSsoUrl() {
		bind("spring.security.saml2.relyingparty.registration.simplesamlphp.assertingparty.single-sign-on.url",
				"https://simplesaml-for-spring-saml/SSOService.php");
		assertThat(
				this.properties.getRegistration().get("simplesamlphp").getAssertingparty().getSinglesignon().getUrl())
			.isEqualTo("https://simplesaml-for-spring-saml/SSOService.php");
	}

	@Test
	void customizeSsoBinding() {
		bind("spring.security.saml2.relyingparty.registration.simplesamlphp.assertingparty.single-sign-on.binding",
				"post");
		assertThat(this.properties.getRegistration()
			.get("simplesamlphp")
			.getAssertingparty()
			.getSinglesignon()
			.getBinding()).isEqualTo(Saml2MessageBinding.POST);
	}

	@Test
	void customizeSsoSignRequests() {
		bind("spring.security.saml2.relyingparty.registration.simplesamlphp.assertingparty.single-sign-on.sign-request",
				"false");
		assertThat(this.properties.getRegistration()
			.get("simplesamlphp")
			.getAssertingparty()
			.getSinglesignon()
			.getSignRequest()).isFalse();
	}

	@Test
	void customizeRelyingPartyEntityId() {
		bind("spring.security.saml2.relyingparty.registration.simplesamlphp.entity-id",
				"{baseUrl}/saml2/custom-entity-id");
		assertThat(this.properties.getRegistration().get("simplesamlphp").getEntityId())
			.isEqualTo("{baseUrl}/saml2/custom-entity-id");
	}

	@Test
	void customizeRelyingPartyEntityIdDefaultsToServiceProviderMetadata() {
		assertThat(RelyingPartyRegistration.withRegistrationId("id")).extracting("entityId")
			.isEqualTo(new Saml2RelyingPartyProperties.Registration().getEntityId());
	}

	@Test
	void customizeAssertingPartyMetadataUri() {
		bind("spring.security.saml2.relyingparty.registration.simplesamlphp.assertingparty.metadata-uri",
				"https://idp.example.org/metadata");
		assertThat(this.properties.getRegistration().get("simplesamlphp").getAssertingparty().getMetadataUri())
			.isEqualTo("https://idp.example.org/metadata");
	}

	@Test
	void customizeSsoSignRequestsIsNullByDefault() {
		this.properties.getRegistration().put("simplesamlphp", new Saml2RelyingPartyProperties.Registration());
		assertThat(this.properties.getRegistration()
			.get("simplesamlphp")
			.getAssertingparty()
			.getSinglesignon()
			.getSignRequest()).isNull();
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.security.saml2.relyingparty", Bindable.ofInstance(this.properties));
	}

}
