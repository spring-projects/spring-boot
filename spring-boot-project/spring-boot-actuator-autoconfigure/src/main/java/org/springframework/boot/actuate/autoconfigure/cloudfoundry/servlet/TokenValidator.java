/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;
import org.springframework.util.Base64Utils;

/**
 * Validator used to ensure that a signed {@link Token} has not been tampered with.
 *
 * @author Madhura Bhave
 */
class TokenValidator {

	private final CloudFoundrySecurityService securityService;

	private Map<String, String> tokenKeys;

	TokenValidator(CloudFoundrySecurityService cloudFoundrySecurityService) {
		this.securityService = cloudFoundrySecurityService;
	}

	public void validate(Token token) {
		validateAlgorithm(token);
		validateKeyIdAndSignature(token);
		validateExpiry(token);
		validateIssuer(token);
		validateAudience(token);
	}

	private void validateAlgorithm(Token token) {
		String algorithm = token.getSignatureAlgorithm();
		if (algorithm == null) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_SIGNATURE,
					"Signing algorithm cannot be null");
		}
		if (!algorithm.equals("RS256")) {
			throw new CloudFoundryAuthorizationException(
					Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM,
					"Signing algorithm " + algorithm + " not supported");
		}
	}

	private void validateKeyIdAndSignature(Token token) {
		String keyId = token.getKeyId();
		if (this.tokenKeys == null || !hasValidKeyId(keyId)) {
			this.tokenKeys = this.securityService.fetchTokenKeys();
			if (!hasValidKeyId(keyId)) {
				throw new CloudFoundryAuthorizationException(Reason.INVALID_KEY_ID,
						"Key Id present in token header does not match");
			}
		}

		if (!hasValidSignature(token, this.tokenKeys.get(keyId))) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_SIGNATURE,
					"RSA Signature did not match content");
		}
	}

	private boolean hasValidKeyId(String tokenKey) {
		return this.tokenKeys.containsKey(tokenKey);
	}

	private boolean hasValidSignature(Token token, String key) {
		try {
			PublicKey publicKey = getPublicKey(key);
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(publicKey);
			signature.update(token.getContent());
			return signature.verify(token.getSignature());
		}
		catch (GeneralSecurityException ex) {
			return false;
		}
	}

	private PublicKey getPublicKey(String key)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		key = key.replace("-----BEGIN PUBLIC KEY-----\n", "");
		key = key.replace("-----END PUBLIC KEY-----", "");
		key = key.trim().replace("\n", "");
		byte[] bytes = Base64Utils.decodeFromString(key);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
		return KeyFactory.getInstance("RSA").generatePublic(keySpec);
	}

	private void validateExpiry(Token token) {
		long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		if (currentTime > token.getExpiry()) {
			throw new CloudFoundryAuthorizationException(Reason.TOKEN_EXPIRED,
					"Token expired");
		}
	}

	private void validateIssuer(Token token) {
		String uaaUrl = this.securityService.getUaaUrl();
		String issuerUri = String.format("%s/oauth/token", uaaUrl);
		if (!issuerUri.equals(token.getIssuer())) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_ISSUER,
					"Token issuer does not match " + uaaUrl + "/oauth/token");
		}
	}

	private void validateAudience(Token token) {
		if (!token.getScope().contains("actuator.read")) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_AUDIENCE,
					"Token does not have audience actuator");
		}

	}

}
