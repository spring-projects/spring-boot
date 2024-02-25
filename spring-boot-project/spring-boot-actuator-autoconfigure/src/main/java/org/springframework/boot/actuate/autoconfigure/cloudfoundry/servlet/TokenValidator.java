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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;

/**
 * Validator used to ensure that a signed {@link Token} has not been tampered with.
 *
 * @author Madhura Bhave
 */
class TokenValidator {

	private final CloudFoundrySecurityService securityService;

	private Map<String, String> tokenKeys;

	/**
     * Constructs a new TokenValidator with the specified CloudFoundrySecurityService.
     * 
     * @param cloudFoundrySecurityService the CloudFoundrySecurityService to be used for token validation
     */
    TokenValidator(CloudFoundrySecurityService cloudFoundrySecurityService) {
		this.securityService = cloudFoundrySecurityService;
	}

	/**
     * Validates the given token.
     * 
     * @param token the token to be validated
     */
    void validate(Token token) {
		validateAlgorithm(token);
		validateKeyIdAndSignature(token);
		validateExpiry(token);
		validateIssuer(token);
		validateAudience(token);
	}

	/**
     * Validates the algorithm used for token signature.
     * 
     * @param token the token to be validated
     * @throws CloudFoundryAuthorizationException if the algorithm is invalid or unsupported
     */
    private void validateAlgorithm(Token token) {
		String algorithm = token.getSignatureAlgorithm();
		if (algorithm == null) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_SIGNATURE, "Signing algorithm cannot be null");
		}
		if (!algorithm.equals("RS256")) {
			throw new CloudFoundryAuthorizationException(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM,
					"Signing algorithm " + algorithm + " not supported");
		}
	}

	/**
     * Validates the key ID and signature of a token.
     * 
     * @param token The token to be validated.
     * @throws CloudFoundryAuthorizationException If the key ID is invalid or the RSA signature does not match the content.
     */
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

	/**
     * Checks if the given token key is a valid key ID.
     * 
     * @param tokenKey the token key to be checked
     * @return true if the token key is a valid key ID, false otherwise
     */
    private boolean hasValidKeyId(String tokenKey) {
		return this.tokenKeys.containsKey(tokenKey);
	}

	/**
     * Checks if the given token has a valid signature using the provided key.
     * 
     * @param token The token to be validated.
     * @param key The key used to verify the token's signature.
     * @return {@code true} if the token has a valid signature, {@code false} otherwise.
     */
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

	/**
     * Retrieves the public key from the given string representation.
     * 
     * @param key the string representation of the public key
     * @return the PublicKey object representing the public key
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     * @throws InvalidKeySpecException if the provided key specification is invalid
     */
    private PublicKey getPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		key = key.replace("-----BEGIN PUBLIC KEY-----\n", "");
		key = key.replace("-----END PUBLIC KEY-----", "");
		key = key.trim().replace("\n", "");
		byte[] bytes = Base64.getDecoder().decode(key);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
		return KeyFactory.getInstance("RSA").generatePublic(keySpec);
	}

	/**
     * Validates the expiry of a given token.
     * 
     * @param token the token to be validated
     * @throws CloudFoundryAuthorizationException if the token has expired
     */
    private void validateExpiry(Token token) {
		long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		if (currentTime > token.getExpiry()) {
			throw new CloudFoundryAuthorizationException(Reason.TOKEN_EXPIRED, "Token expired");
		}
	}

	/**
     * Validates the issuer of the given token.
     * 
     * @param token The token to be validated.
     * @throws CloudFoundryAuthorizationException If the token issuer does not match the expected issuer URI.
     */
    private void validateIssuer(Token token) {
		String uaaUrl = this.securityService.getUaaUrl();
		String issuerUri = String.format("%s/oauth/token", uaaUrl);
		if (!issuerUri.equals(token.getIssuer())) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_ISSUER,
					"Token issuer does not match " + uaaUrl + "/oauth/token");
		}
	}

	/**
     * Validates the audience of the given token.
     * 
     * @param token the token to be validated
     * @throws CloudFoundryAuthorizationException if the token does not have the required audience
     */
    private void validateAudience(Token token) {
		if (!token.getScope().contains("actuator.read")) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_AUDIENCE,
					"Token does not have audience actuator");
		}

	}

}
