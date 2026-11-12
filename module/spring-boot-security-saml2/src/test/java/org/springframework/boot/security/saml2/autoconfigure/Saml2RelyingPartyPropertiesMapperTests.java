/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.saml2.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties.AssertingParty.Verification;
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties.Decryption;
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties.Registration;
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties.Registration.Signing;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Saml2RelyingPartyPropertiesMapper}.
 *
 * @author Lee Taeho
 */
class Saml2RelyingPartyPropertiesMapperTests {

	@Test
	void asRelyingPartyRegistrationsWhenNoRegistrationsReturnsEmptyMap() {
		Saml2RelyingPartyProperties properties = new Saml2RelyingPartyProperties();
		assertThat(new Saml2RelyingPartyPropertiesMapper(properties).asRelyingPartyRegistrations()).isEmpty();
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void asRelyingPartyRegistrationsMapsProperties() {
		Saml2RelyingPartyProperties properties = new Saml2RelyingPartyProperties();
		Registration registration = createRegistration();
		Signing.Credential signingCredential = new Signing.Credential();
		signingCredential.setPrivateKeyLocation(new ClassPathResource("private-key-location"));
		signingCredential.setCertificateLocation(new ClassPathResource("certificate-location"));
		registration.getSigning().getCredentials().add(signingCredential);
		properties.getRegistration().put("foo", registration);
		Map<String, RelyingPartyRegistration> registrations = new Saml2RelyingPartyPropertiesMapper(properties)
			.asRelyingPartyRegistrations();
		assertThat(registrations).containsOnlyKeys("foo");
		RelyingPartyRegistration relyingPartyRegistration = registrations.get("foo");
		assertThat(relyingPartyRegistration).isNotNull();
		assertThat(relyingPartyRegistration.getRegistrationId()).isEqualTo("foo");
		assertThat(relyingPartyRegistration.getEntityId()).isEqualTo("{baseUrl}/saml2/foo-entity-id");
		assertThat(relyingPartyRegistration.getAssertionConsumerServiceLocation())
			.isEqualTo("{baseUrl}/login/saml2/foo-entity-id");
		assertThat(relyingPartyRegistration.getAssertionConsumerServiceBinding())
			.isEqualTo(Saml2MessageBinding.REDIRECT);
		assertThat(relyingPartyRegistration.getAssertingPartyMetadata().getEntityId())
			.isEqualTo("https://idp.example.com/metadata");
		assertThat(relyingPartyRegistration.getAssertingPartyMetadata().getSingleSignOnServiceLocation())
			.isEqualTo("https://idp.example.com/sso");
		assertThat(relyingPartyRegistration.getAssertingPartyMetadata().getVerificationX509Credentials()).hasSize(1);
		assertThat(relyingPartyRegistration.getSigningX509Credentials()).hasSize(1);
	}

	@Test
	@WithPackageResources("certificate-location")
	void asRelyingPartyRegistrationsWhenMultipleRegistrationsPreservesOrder() {
		Saml2RelyingPartyProperties properties = new Saml2RelyingPartyProperties();
		properties.getRegistration().put("foo", createRegistration());
		properties.getRegistration().put("bar", createRegistration());
		Map<String, RelyingPartyRegistration> registrations = new Saml2RelyingPartyPropertiesMapper(properties)
			.asRelyingPartyRegistrations();
		assertThat(registrations.keySet()).containsExactly("foo", "bar");
		assertThat(registrations.get("bar")).extracting(RelyingPartyRegistration::getRegistrationId).isEqualTo("bar");
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void asRelyingPartyRegistrationsMapsDecryptionAndSingleLogout() {
		Saml2RelyingPartyProperties properties = new Saml2RelyingPartyProperties();
		Registration registration = createRegistration();
		Decryption.Credential decryptionCredential = new Decryption.Credential();
		decryptionCredential.setPrivateKeyLocation(new ClassPathResource("private-key-location"));
		decryptionCredential.setCertificateLocation(new ClassPathResource("certificate-location"));
		registration.getDecryption().getCredentials().add(decryptionCredential);
		registration.getSinglelogout().setUrl("{baseUrl}/logout/saml2/slo");
		registration.getSinglelogout().setResponseUrl("{baseUrl}/logout/saml2/slo/response");
		registration.getAssertingparty().getSinglelogout().setUrl("https://idp.example.com/slo");
		properties.getRegistration().put("foo", registration);
		RelyingPartyRegistration relyingPartyRegistration = new Saml2RelyingPartyPropertiesMapper(properties)
			.asRelyingPartyRegistrations()
			.get("foo");
		assertThat(relyingPartyRegistration).isNotNull();
		assertThat(relyingPartyRegistration.getDecryptionX509Credentials()).hasSize(1);
		assertThat(relyingPartyRegistration.getSingleLogoutServiceLocation()).isEqualTo("{baseUrl}/logout/saml2/slo");
		assertThat(relyingPartyRegistration.getSingleLogoutServiceResponseLocation())
			.isEqualTo("{baseUrl}/logout/saml2/slo/response");
		assertThat(relyingPartyRegistration.getSingleLogoutServiceBindings()).containsExactly(Saml2MessageBinding.POST);
		assertThat(relyingPartyRegistration.getAssertingPartyMetadata().getSingleLogoutServiceLocation())
			.isEqualTo("https://idp.example.com/slo");
	}

	@Test
	@WithPackageResources("certificate-location")
	void asRelyingPartyRegistrationsWhenSignRequestsTrueAndNoSigningCredentialsThrowsException() {
		Saml2RelyingPartyProperties properties = new Saml2RelyingPartyProperties();
		Registration registration = createRegistration();
		registration.getAssertingparty().getSinglesignon().setSignRequest(true);
		properties.getRegistration().put("foo", registration);
		assertThatIllegalStateException()
			.isThrownBy(() -> new Saml2RelyingPartyPropertiesMapper(properties).asRelyingPartyRegistrations())
			.withMessage("Signing credentials must not be empty when authentication requests require signing.");
	}

	private Registration createRegistration() {
		Registration registration = new Registration();
		registration.setEntityId("{baseUrl}/saml2/foo-entity-id");
		registration.getAcs().setLocation("{baseUrl}/login/saml2/foo-entity-id");
		registration.getAcs().setBinding(Saml2MessageBinding.REDIRECT);
		registration.getAssertingparty().setEntityId("https://idp.example.com/metadata");
		registration.getAssertingparty().getSinglesignon().setUrl("https://idp.example.com/sso");
		registration.getAssertingparty().getSinglesignon().setSignRequest(false);
		Verification.Credential verificationCredential = new Verification.Credential();
		verificationCredential.setCertificateLocation(new ClassPathResource("certificate-location"));
		registration.getAssertingparty().getVerification().getCredentials().add(verificationCredential);
		return registration;
	}

}
