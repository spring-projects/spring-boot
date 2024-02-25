/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

/**
 * SAML2 relying party properties.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Lasse Wulff
 * @since 2.2.0
 */
@ConfigurationProperties("spring.security.saml2.relyingparty")
public class Saml2RelyingPartyProperties {

	/**
	 * SAML2 relying party registrations.
	 */
	private final Map<String, Registration> registration = new LinkedHashMap<>();

	/**
	 * Returns the map of registrations.
	 * @return the map of registrations
	 */
	public Map<String, Registration> getRegistration() {
		return this.registration;
	}

	/**
	 * Represents a SAML Relying Party.
	 */
	public static class Registration {

		/**
		 * Relying party's entity ID. The value may contain a number of placeholders. They
		 * are "baseUrl", "registrationId", "baseScheme", "baseHost", and "basePort".
		 */
		private String entityId = "{baseUrl}/saml2/service-provider-metadata/{registrationId}";

		/**
		 * Assertion Consumer Service.
		 */
		private final Acs acs = new Acs();

		private final Signing signing = new Signing();

		private final Decryption decryption = new Decryption();

		private final Singlelogout singlelogout = new Singlelogout();

		/**
		 * Remote SAML Identity Provider.
		 */
		private final AssertingParty assertingparty = new AssertingParty();

		private String nameIdFormat;

		/**
		 * Returns the entity ID of the Registration.
		 * @return the entity ID of the Registration
		 */
		public String getEntityId() {
			return this.entityId;
		}

		/**
		 * Sets the entity ID for the registration.
		 * @param entityId the entity ID to be set
		 */
		public void setEntityId(String entityId) {
			this.entityId = entityId;
		}

		/**
		 * Returns the Acs object associated with this Registration.
		 * @return the Acs object associated with this Registration
		 */
		public Acs getAcs() {
			return this.acs;
		}

		/**
		 * Returns the signing object associated with this Registration.
		 * @return the signing object
		 */
		public Signing getSigning() {
			return this.signing;
		}

		/**
		 * Returns the decryption object used for decryption operations.
		 * @return the decryption object
		 */
		public Decryption getDecryption() {
			return this.decryption;
		}

		/**
		 * Returns the Singlelogout object associated with this Registration.
		 * @return the Singlelogout object associated with this Registration
		 */
		public Singlelogout getSinglelogout() {
			return this.singlelogout;
		}

		/**
		 * Returns the asserting party associated with this registration.
		 * @return the asserting party
		 */
		public AssertingParty getAssertingparty() {
			return this.assertingparty;
		}

		/**
		 * Returns the name ID format.
		 * @return the name ID format
		 */
		public String getNameIdFormat() {
			return this.nameIdFormat;
		}

		/**
		 * Sets the NameID format for the registration.
		 * @param nameIdFormat the NameID format to be set
		 */
		public void setNameIdFormat(String nameIdFormat) {
			this.nameIdFormat = nameIdFormat;
		}

		/**
		 * Acs class.
		 */
		public static class Acs {

			/**
			 * Assertion Consumer Service location template. Can generate its location
			 * based on possible variables of "baseUrl", "registrationId", "baseScheme",
			 * "baseHost", and "basePort".
			 */
			private String location = "{baseUrl}/login/saml2/sso/{registrationId}";

			/**
			 * Assertion Consumer Service binding.
			 */
			private Saml2MessageBinding binding = Saml2MessageBinding.POST;

			/**
			 * Returns the location of the Acs object.
			 * @return the location of the Acs object
			 */
			public String getLocation() {
				return this.location;
			}

			/**
			 * Sets the location of the Acs.
			 * @param location the new location of the Acs
			 */
			public void setLocation(String location) {
				this.location = location;
			}

			/**
			 * Returns the SAML 2.0 message binding used by this Acs instance.
			 * @return the SAML 2.0 message binding
			 */
			public Saml2MessageBinding getBinding() {
				return this.binding;
			}

			/**
			 * Sets the binding for the SAML 2 message.
			 * @param binding the SAML2MessageBinding to set
			 */
			public void setBinding(Saml2MessageBinding binding) {
				this.binding = binding;
			}

		}

		/**
		 * Signing class.
		 */
		public static class Signing {

			/**
			 * Credentials used for signing the SAML authentication request.
			 */
			private List<Credential> credentials = new ArrayList<>();

			/**
			 * Retrieves the list of credentials.
			 * @return the list of credentials
			 */
			public List<Credential> getCredentials() {
				return this.credentials;
			}

			/**
			 * Sets the list of credentials for signing.
			 * @param credentials the list of credentials to be set
			 */
			public void setCredentials(List<Credential> credentials) {
				this.credentials = credentials;
			}

			/**
			 * Credential class.
			 */
			public static class Credential {

				/**
				 * Private key used for signing.
				 */
				private Resource privateKeyLocation;

				/**
				 * Relying Party X509Certificate shared with the identity provider.
				 */
				private Resource certificateLocation;

				/**
				 * Returns the location of the private key.
				 * @return the location of the private key
				 */
				public Resource getPrivateKeyLocation() {
					return this.privateKeyLocation;
				}

				/**
				 * Sets the location of the private key.
				 * @param privateKey the resource representing the private key location
				 */
				public void setPrivateKeyLocation(Resource privateKey) {
					this.privateKeyLocation = privateKey;
				}

				/**
				 * Returns the location of the certificate.
				 * @return the location of the certificate
				 */
				public Resource getCertificateLocation() {
					return this.certificateLocation;
				}

				/**
				 * Sets the location of the certificate.
				 * @param certificate the resource representing the certificate location
				 */
				public void setCertificateLocation(Resource certificate) {
					this.certificateLocation = certificate;
				}

			}

		}

	}

	/**
	 * Decryption class.
	 */
	public static class Decryption {

		/**
		 * Credentials used for decrypting the SAML authentication request.
		 */
		private List<Credential> credentials = new ArrayList<>();

		/**
		 * Retrieves the list of credentials.
		 * @return the list of credentials
		 */
		public List<Credential> getCredentials() {
			return this.credentials;
		}

		/**
		 * Sets the list of credentials for decryption.
		 * @param credentials the list of credentials to be set
		 */
		public void setCredentials(List<Credential> credentials) {
			this.credentials = credentials;
		}

		/**
		 * Credential class.
		 */
		public static class Credential {

			/**
			 * Private key used for decrypting.
			 */
			private Resource privateKeyLocation;

			/**
			 * Relying Party X509Certificate shared with the identity provider.
			 */
			private Resource certificateLocation;

			/**
			 * Returns the location of the private key.
			 * @return the location of the private key
			 */
			public Resource getPrivateKeyLocation() {
				return this.privateKeyLocation;
			}

			/**
			 * Sets the location of the private key.
			 * @param privateKey the resource representing the private key location
			 */
			public void setPrivateKeyLocation(Resource privateKey) {
				this.privateKeyLocation = privateKey;
			}

			/**
			 * Returns the location of the certificate.
			 * @return the location of the certificate
			 */
			public Resource getCertificateLocation() {
				return this.certificateLocation;
			}

			/**
			 * Sets the location of the certificate.
			 * @param certificate the resource representing the certificate location
			 */
			public void setCertificateLocation(Resource certificate) {
				this.certificateLocation = certificate;
			}

		}

	}

	/**
	 * Represents a remote Identity Provider.
	 */
	public static class AssertingParty {

		/**
		 * Unique identifier for the identity provider.
		 */
		private String entityId;

		/**
		 * URI to the metadata endpoint for discovery-based configuration.
		 */
		private String metadataUri;

		private final Singlesignon singlesignon = new Singlesignon();

		private final Verification verification = new Verification();

		private final Singlelogout singlelogout = new Singlelogout();

		/**
		 * Returns the entity ID of the asserting party.
		 * @return the entity ID of the asserting party
		 */
		public String getEntityId() {
			return this.entityId;
		}

		/**
		 * Sets the entity ID for the asserting party.
		 * @param entityId the entity ID to be set
		 */
		public void setEntityId(String entityId) {
			this.entityId = entityId;
		}

		/**
		 * Returns the metadata URI of the asserting party.
		 * @return the metadata URI of the asserting party
		 */
		public String getMetadataUri() {
			return this.metadataUri;
		}

		/**
		 * Sets the metadata URI for the asserting party.
		 * @param metadataUri the metadata URI to be set
		 */
		public void setMetadataUri(String metadataUri) {
			this.metadataUri = metadataUri;
		}

		/**
		 * Returns the Singlesignon object associated with this AssertingParty.
		 * @return the Singlesignon object associated with this AssertingParty
		 */
		public Singlesignon getSinglesignon() {
			return this.singlesignon;
		}

		/**
		 * Returns the verification object associated with this asserting party.
		 * @return the verification object
		 */
		public Verification getVerification() {
			return this.verification;
		}

		/**
		 * Returns the Singlelogout object associated with this AssertingParty.
		 * @return the Singlelogout object associated with this AssertingParty
		 */
		public Singlelogout getSinglelogout() {
			return this.singlelogout;
		}

		/**
		 * Single sign on details for an Identity Provider.
		 */
		public static class Singlesignon {

			/**
			 * Remote endpoint to send authentication requests to.
			 */
			private String url;

			/**
			 * Whether to redirect or post authentication requests.
			 */
			private Saml2MessageBinding binding;

			/**
			 * Whether to sign authentication requests.
			 */
			private Boolean signRequest;

			/**
			 * Returns the URL associated with the Singlesignon object.
			 * @return the URL associated with the Singlesignon object
			 */
			public String getUrl() {
				return this.url;
			}

			/**
			 * Sets the URL for the Single Sign-On.
			 * @param url the URL to be set for the Single Sign-On
			 */
			public void setUrl(String url) {
				this.url = url;
			}

			/**
			 * Returns the SAML 2.0 message binding used for Single Sign-On.
			 * @return the SAML 2.0 message binding
			 */
			public Saml2MessageBinding getBinding() {
				return this.binding;
			}

			/**
			 * Sets the binding for the SAML 2.0 message.
			 * @param binding the SAML 2.0 message binding to be set
			 */
			public void setBinding(Saml2MessageBinding binding) {
				this.binding = binding;
			}

			/**
			 * Returns a boolean value indicating whether the request is a sign request.
			 * @return true if the request is a sign request, false otherwise
			 */
			public boolean isSignRequest() {
				return this.signRequest;
			}

			/**
			 * Returns the sign request flag.
			 * @return the sign request flag
			 */
			public Boolean getSignRequest() {
				return this.signRequest;
			}

			/**
			 * Sets the sign request flag.
			 * @param signRequest the sign request flag to be set
			 */
			public void setSignRequest(Boolean signRequest) {
				this.signRequest = signRequest;
			}

		}

		/**
		 * Verification details for an Identity Provider.
		 */
		public static class Verification {

			/**
			 * Credentials used for verification of incoming SAML messages.
			 */
			private List<Credential> credentials = new ArrayList<>();

			/**
			 * Retrieves the list of credentials.
			 * @return the list of credentials
			 */
			public List<Credential> getCredentials() {
				return this.credentials;
			}

			/**
			 * Sets the list of credentials for verification.
			 * @param credentials the list of credentials to be set
			 */
			public void setCredentials(List<Credential> credentials) {
				this.credentials = credentials;
			}

			/**
			 * Credential class.
			 */
			public static class Credential {

				/**
				 * Locations of the X.509 certificate used for verification of incoming
				 * SAML messages.
				 */
				private Resource certificate;

				/**
				 * Returns the location of the certificate.
				 * @return the location of the certificate
				 */
				public Resource getCertificateLocation() {
					return this.certificate;
				}

				/**
				 * Sets the location of the certificate.
				 * @param certificate the resource representing the certificate location
				 */
				public void setCertificateLocation(Resource certificate) {
					this.certificate = certificate;
				}

			}

		}

	}

	/**
	 * Single logout details.
	 */
	public static class Singlelogout {

		/**
		 * Location where SAML2 LogoutRequest gets sent to.
		 */
		private String url;

		/**
		 * Location where SAML2 LogoutResponse gets sent to.
		 */
		private String responseUrl;

		/**
		 * Whether to redirect or post logout requests.
		 */
		private Saml2MessageBinding binding;

		/**
		 * Returns the URL associated with the Singlelogout instance.
		 * @return the URL associated with the Singlelogout instance
		 */
		public String getUrl() {
			return this.url;
		}

		/**
		 * Sets the URL for the Singlelogout class.
		 * @param url the URL to be set
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 * Returns the response URL.
		 * @return the response URL
		 */
		public String getResponseUrl() {
			return this.responseUrl;
		}

		/**
		 * Sets the response URL for the Singlelogout class.
		 * @param responseUrl the response URL to be set
		 */
		public void setResponseUrl(String responseUrl) {
			this.responseUrl = responseUrl;
		}

		/**
		 * Returns the SAML 2.0 message binding used for the Single Logout functionality.
		 * @return the SAML 2.0 message binding
		 */
		public Saml2MessageBinding getBinding() {
			return this.binding;
		}

		/**
		 * Sets the binding for the SAML 2.0 Single Logout message.
		 * @param binding the SAML 2.0 message binding to be set
		 */
		public void setBinding(Saml2MessageBinding binding) {
			this.binding = binding;
		}

	}

}
