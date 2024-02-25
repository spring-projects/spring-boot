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
 * @author Lasse Wulff
 */
@Configuration(proxyBeanMethods = false)
@Conditional(RegistrationConfiguredCondition.class)
@ConditionalOnMissingBean(RelyingPartyRegistrationRepository.class)
class Saml2RelyingPartyRegistrationConfiguration {

	/**
     * Creates a new instance of RelyingPartyRegistrationRepository based on the provided Saml2RelyingPartyProperties.
     * 
     * @param properties the Saml2RelyingPartyProperties containing the configuration for the relying party registrations
     * @return a new instance of RelyingPartyRegistrationRepository
     */
    @Bean
	RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(Saml2RelyingPartyProperties properties) {
		List<RelyingPartyRegistration> registrations = properties.getRegistration()
			.entrySet()
			.stream()
			.map(this::asRegistration)
			.toList();
		return new InMemoryRelyingPartyRegistrationRepository(registrations);
	}

	/**
     * Converts a Map.Entry object containing a key-value pair of String and Registration into a RelyingPartyRegistration object.
     * 
     * @param entry the Map.Entry object containing the key-value pair
     * @return the converted RelyingPartyRegistration object
     */
    private RelyingPartyRegistration asRegistration(Map.Entry<String, Registration> entry) {
		return asRegistration(entry.getKey(), entry.getValue());
	}

	/**
     * Converts the given registration properties into a {@link RelyingPartyRegistration} object.
     * 
     * @param id the registration ID
     * @param properties the registration properties
     * @return the converted {@link RelyingPartyRegistration} object
     */
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
		builder.nameIdFormat(properties.getNameIdFormat());
		RelyingPartyRegistration registration = builder.build();
		boolean signRequest = registration.getAssertingPartyDetails().getWantAuthnRequestsSigned();
		validateSigningCredentials(properties, signRequest);
		return registration;
	}

	/**
     * Creates a RelyingPartyRegistration.Builder using the metadata provided by the AssertingParty.
     * 
     * @param properties The AssertingParty properties containing the required metadata.
     * @return The RelyingPartyRegistration.Builder created using the metadata.
     * @throws IllegalStateException If no relying party with the specified Entity ID is found.
     */
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

	/**
     * Retrieves the entity ID from the provided RelyingPartyRegistration.Builder object.
     * 
     * @param candidate the RelyingPartyRegistration.Builder object to extract the entity ID from
     * @return the entity ID as an Object
     */
    private Object getEntityId(RelyingPartyRegistration.Builder candidate) {
		String[] result = new String[1];
		candidate.assertingPartyDetails((builder) -> result[0] = builder.build().getEntityId());
		return result[0];
	}

	/**
     * Maps the properties of an AssertingParty object to an AssertingPartyDetails.Builder object.
     * 
     * @param assertingParty the AssertingParty object to be mapped
     * @return a Consumer function that maps the properties of the AssertingParty object to the AssertingPartyDetails.Builder object
     */
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

	/**
     * Validates the signing credentials for the given registration properties.
     * 
     * @param properties the registration properties
     * @param signRequest flag indicating whether the authentication requests require signing
     * 
     * @throws IllegalArgumentException if signRequest is true and the signing credentials are empty
     */
    private void validateSigningCredentials(Registration properties, boolean signRequest) {
		if (signRequest) {
			Assert.state(!properties.getSigning().getCredentials().isEmpty(),
					"Signing credentials must not be empty when authentication requests require signing.");
		}
	}

	/**
     * Returns a Saml2X509Credential object as a signing credential.
     * 
     * @param properties the Signing.Credential object containing the private key and certificate locations
     * @return a Saml2X509Credential object configured as a signing credential
     */
    private Saml2X509Credential asSigningCredential(Signing.Credential properties) {
		RSAPrivateKey privateKey = readPrivateKey(properties.getPrivateKeyLocation());
		X509Certificate certificate = readCertificate(properties.getCertificateLocation());
		return new Saml2X509Credential(privateKey, certificate, Saml2X509CredentialType.SIGNING);
	}

	/**
     * Returns a Saml2X509Credential object for decryption based on the provided properties.
     *
     * @param properties The Decryption.Credential object containing the private key and certificate locations.
     * @return A Saml2X509Credential object for decryption.
     */
    private Saml2X509Credential asDecryptionCredential(Decryption.Credential properties) {
		RSAPrivateKey privateKey = readPrivateKey(properties.getPrivateKeyLocation());
		X509Certificate certificate = readCertificate(properties.getCertificateLocation());
		return new Saml2X509Credential(privateKey, certificate, Saml2X509CredentialType.DECRYPTION);
	}

	/**
     * Converts the given Verification.Credential properties into a Saml2X509Credential object for verification purposes.
     * 
     * @param properties the Verification.Credential properties containing the certificate location
     * @return the Saml2X509Credential object for verification
     */
    private Saml2X509Credential asVerificationCredential(Verification.Credential properties) {
		X509Certificate certificate = readCertificate(properties.getCertificateLocation());
		return new Saml2X509Credential(certificate, Saml2X509Credential.Saml2X509CredentialType.ENCRYPTION,
				Saml2X509Credential.Saml2X509CredentialType.VERIFICATION);
	}

	/**
     * Reads a private key from the specified resource location.
     *
     * @param location the resource location of the private key
     * @return the RSAPrivateKey object representing the private key
     * @throws IllegalArgumentException if the private key location is not specified or does not exist,
     *                                  or if an error occurs while reading the private key
     */
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

	/**
     * Reads an X509Certificate from the specified resource location.
     *
     * @param location the resource location of the certificate
     * @return the X509Certificate read from the location
     * @throws IllegalArgumentException if the location is null or does not exist, or if an error occurs while reading the certificate
     */
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
