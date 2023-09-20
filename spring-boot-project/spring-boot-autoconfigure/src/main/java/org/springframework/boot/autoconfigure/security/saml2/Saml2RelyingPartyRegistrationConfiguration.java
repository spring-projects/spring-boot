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

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty.Verification;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Decryption;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Registration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Registration.Signing;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.Builder;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Configuration @Configuration} used to map {@link Saml2RelyingPartyProperties} to
 * relying party registrations in a {@link RelyingPartyRegistrationRepository}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Lasse Lindqvist
 */
@Configuration(proxyBeanMethods = false)
@Conditional(RegistrationConfiguredCondition.class)
@ConditionalOnMissingBean(RelyingPartyRegistrationRepository.class)
class Saml2RelyingPartyRegistrationConfiguration {

	@Bean
	RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(Saml2RelyingPartyProperties properties) {
		List<RelyingPartyRegistration> registrations = properties.getRegistration()
			.entrySet()
			.stream()
			.map(this::asRegistration)
			.toList();
		return new InMemoryRelyingPartyRegistrationRepository(registrations);
	}

	private RelyingPartyRegistration asRegistration(Map.Entry<String, Registration> entry) {
		return asRegistration(entry.getKey(), entry.getValue());
	}

	private RelyingPartyRegistration asRegistration(String id, Registration properties) {
		boolean usingMetadata = StringUtils.hasText(properties.getAssertingparty().getMetadataUri());
		Builder builder = (!usingMetadata) ? RelyingPartyRegistration.withRegistrationId(id)
				: createBuilderUsingMetadata(properties.getAssertingparty()).registrationId(id);
		builder.assertionConsumerServiceLocation(properties.getAcs().getLocation());
		builder.assertionConsumerServiceBinding(properties.getAcs().getBinding());
		builder.assertingPartyDetails(mapAssertingParty(properties.getAssertingparty()));
		builder.signingX509Credentials((credentials) -> properties.getSigning()
			.getCredentials()
			.stream()
			.map(this::asSigningCredential)
			.forEach(credentials::add));
		builder.decryptionX509Credentials((credentials) -> properties.getDecryption()
			.getCredentials()
			.stream()
			.map(this::asDecryptionCredential)
			.forEach(credentials::add));
		builder.assertingPartyDetails(
				(details) -> details.verificationX509Credentials((credentials) -> properties.getAssertingparty()
					.getVerification()
					.getCredentials()
					.stream()
					.map(this::asVerificationCredential)
					.forEach(credentials::add)));
		builder.singleLogoutServiceLocation(properties.getSinglelogout().getUrl());
		builder.singleLogoutServiceResponseLocation(properties.getSinglelogout().getResponseUrl());
		builder.singleLogoutServiceBinding(properties.getSinglelogout().getBinding());
		builder.entityId(properties.getEntityId());
		RelyingPartyRegistration registration = builder.build();
		boolean signRequest = registration.getAssertingPartyDetails().getWantAuthnRequestsSigned();
		validateSigningCredentials(properties, signRequest);
		return registration;
	}

	private RelyingPartyRegistration.Builder createBuilderUsingMetadata(AssertingParty properties) {
		String requiredEntityId = properties.getEntityId();
		Collection<Builder> candidates = RelyingPartyRegistrations
			.collectionFromMetadataLocation(properties.getMetadataUri());
		for (RelyingPartyRegistration.Builder candidate : candidates) {
			if (requiredEntityId == null || requiredEntityId.equals(getEntityId(candidate))) {
				return candidate;
			}
		}
		throw new IllegalStateException("No relying party with Entity ID '" + requiredEntityId + "' found");
	}

	private Object getEntityId(RelyingPartyRegistration.Builder candidate) {
		String[] result = new String[1];
		candidate.assertingPartyDetails((builder) -> result[0] = builder.build().getEntityId());
		return result[0];
	}

	private Consumer<AssertingPartyDetails.Builder> mapAssertingParty(AssertingParty assertingParty) {
		return (details) -> {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(assertingParty::getEntityId).to(details::entityId);
			map.from(assertingParty.getSinglesignon()::getBinding).to(details::singleSignOnServiceBinding);
			map.from(assertingParty.getSinglesignon()::getUrl).to(details::singleSignOnServiceLocation);
			map.from(assertingParty.getSinglesignon()::getSignRequest).to(details::wantAuthnRequestsSigned);
			map.from(assertingParty.getSinglelogout()::getUrl).to(details::singleLogoutServiceLocation);
			map.from(assertingParty.getSinglelogout()::getResponseUrl).to(details::singleLogoutServiceResponseLocation);
			map.from(assertingParty.getSinglelogout()::getBinding).to(details::singleLogoutServiceBinding);
		};
	}

	private void validateSigningCredentials(Registration properties, boolean signRequest) {
		if (signRequest) {
			Assert.state(!properties.getSigning().getCredentials().isEmpty(),
					"Signing credentials must not be empty when authentication requests require signing.");
		}
	}

	private Saml2X509Credential asSigningCredential(Signing.Credential properties) {
		RSAPrivateKey privateKey = readPrivateKey(properties.getPrivateKeyLocation());
		X509Certificate certificate = readCertificate(properties.getCertificateLocation());
		return new Saml2X509Credential(privateKey, certificate, Saml2X509CredentialType.SIGNING);
	}

	private Saml2X509Credential asDecryptionCredential(Decryption.Credential properties) {
		RSAPrivateKey privateKey = readPrivateKey(properties.getPrivateKeyLocation());
		X509Certificate certificate = readCertificate(properties.getCertificateLocation());
		return new Saml2X509Credential(privateKey, certificate, Saml2X509CredentialType.DECRYPTION);
	}

	private Saml2X509Credential asVerificationCredential(Verification.Credential properties) {
		X509Certificate certificate = readCertificate(properties.getCertificateLocation());
		return new Saml2X509Credential(certificate, Saml2X509Credential.Saml2X509CredentialType.ENCRYPTION,
				Saml2X509Credential.Saml2X509CredentialType.VERIFICATION);
	}

	private RSAPrivateKey readPrivateKey(Resource location) {
		Assert.state(location != null, "No private key location specified");
		Assert.state(location.exists(), () -> "Private key location '" + location + "' does not exist");
		try (InputStream inputStream = location.getInputStream()) {
			return RsaKeyConverters.pkcs8().convert(inputStream);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private X509Certificate readCertificate(Resource location) {
		Assert.state(location != null, "No certificate location specified");
		Assert.state(location.exists(), () -> "Certificate  location '" + location + "' does not exist");
		try (InputStream inputStream = location.getInputStream()) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}
