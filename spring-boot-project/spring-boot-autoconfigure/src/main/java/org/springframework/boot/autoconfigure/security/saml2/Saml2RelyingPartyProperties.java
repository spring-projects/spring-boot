/*
 * Copyright 2012-2020 the original author or authors.
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
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.core.io.Resource;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

/**
 * SAML2 relying party properties.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.2.0
 */
@ConfigurationProperties("spring.security.saml2.relyingparty")
public class Saml2RelyingPartyProperties {

	/**
	 * SAML2 relying party registrations.
	 */
	private final Map<String, Registration> registration = new LinkedHashMap<>();

	public Map<String, Registration> getRegistration() {
		return this.registration;
	}

	/**
	 * Represents a SAML Relying Party.
	 */
	public static class Registration {

		/**
		 * Relying party's entity ID template. Can generate its entity ID based on
		 * possible variables of "baseUrl", "registrationId", "baseScheme", "baseHost",
		 * and "basePort".
		 */
		private String relyingPartyEntityId = "{baseUrl}/saml2/service-provider-metadata/{registrationId}";

		private final Signing signing = new Signing();

		/**
		 * Remote SAML Identity Provider.
		 */
		private final Identityprovider identityprovider = new Identityprovider();

		public String getRelyingPartyEntityId() {
			return this.relyingPartyEntityId;
		}

		public void setRelyingPartyEntityId(String entityId) {
			this.relyingPartyEntityId = entityId;
		}

		public Signing getSigning() {
			return this.signing;
		}

		public Identityprovider getIdentityprovider() {
			return this.identityprovider;
		}

		public static class Signing {

			/**
			 * Credentials used for signing and decrypting the SAML authentication
			 * request.
			 */
			private List<Credential> credentials = new ArrayList<>();

			public List<Credential> getCredentials() {
				return this.credentials;
			}

			public void setCredentials(List<Credential> credentials) {
				this.credentials = credentials;
			}

			public static class Credential {

				/**
				 * Private key used for signing or decrypting.
				 */
				private Resource privateKeyLocation;

				/**
				 * Relying Party X509Certificate shared with the identity provider.
				 */
				private Resource certificateLocation;

				public Resource getPrivateKeyLocation() {
					return this.privateKeyLocation;
				}

				public void setPrivateKeyLocation(Resource privateKey) {
					this.privateKeyLocation = privateKey;
				}

				public Resource getCertificateLocation() {
					return this.certificateLocation;
				}

				public void setCertificateLocation(Resource certificate) {
					this.certificateLocation = certificate;
				}

			}

		}

	}

	/**
	 * Represents a remote Identity Provider.
	 */
	public static class Identityprovider {

		/**
		 * Unique identifier for the identity provider.
		 */
		private String entityId;

		private final Singlesignon singlesignon = new Singlesignon();

		private final Verification verification = new Verification();

		public String getEntityId() {
			return this.entityId;
		}

		public void setEntityId(String entityId) {
			this.entityId = entityId;
		}

		@Deprecated
		@DeprecatedConfigurationProperty(reason = "moved to 'singlesignon.url'")
		public String getSsoUrl() {
			return this.singlesignon.getUrl();
		}

		@Deprecated
		public void setSsoUrl(String ssoUrl) {
			this.singlesignon.setUrl(ssoUrl);
		}

		public Singlesignon getSinglesignon() {
			return this.singlesignon;
		}

		public Verification getVerification() {
			return this.verification;
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
			private Saml2MessageBinding binding = Saml2MessageBinding.REDIRECT;

			/**
			 * Whether to sign authentication requests.
			 */
			private boolean signRequest = true;

			public String getUrl() {
				return this.url;
			}

			public void setUrl(String url) {
				this.url = url;
			}

			public Saml2MessageBinding getBinding() {
				return this.binding;
			}

			public void setBinding(Saml2MessageBinding binding) {
				this.binding = binding;
			}

			public boolean isSignRequest() {
				return this.signRequest;
			}

			public void setSignRequest(boolean signRequest) {
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

			public List<Credential> getCredentials() {
				return this.credentials;
			}

			public void setCredentials(List<Credential> credentials) {
				this.credentials = credentials;
			}

			public static class Credential {

				/**
				 * Locations of the X.509 certificate used for verification of incoming
				 * SAML messages.
				 */
				private Resource certificate;

				public Resource getCertificateLocation() {
					return this.certificate;
				}

				public void setCertificateLocation(Resource certificate) {
					this.certificate = certificate;
				}

			}

		}

	}

}
