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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;

/**
 * Validator used to ensure that a signed {@link Token} has not been tampered with.
 *
 * @author Madhura Bhave
 */
class ReactiveTokenValidator {

	private final ReactiveCloudFoundrySecurityService securityService;

	private volatile Map<String, String> cachedTokenKeys = Collections.emptyMap();

	/**
     * Constructs a new ReactiveTokenValidator with the specified ReactiveCloudFoundrySecurityService.
     * 
     * @param securityService the ReactiveCloudFoundrySecurityService used for token validation
     */
    ReactiveTokenValidator(ReactiveCloudFoundrySecurityService securityService) {
		this.securityService = securityService;
	}

	/**
     * Validates the given token by performing a series of validation steps.
     *
     * @param token the token to be validated
     * @return a Mono representing the completion of the validation process
     */
    Mono<Void> validate(Token token) {
		return validateAlgorithm(token).then(validateKeyIdAndSignature(token))
			.then(validateExpiry(token))
			.then(validateIssuer(token))
			.then(validateAudience(token));
	}

	/**
     * Validates the algorithm used for token signature.
     * 
     * @param token the token to be validated
     * @return a Mono<Void> indicating the completion of the validation process
     * @throws CloudFoundryAuthorizationException if the algorithm is invalid or unsupported
     */
    private Mono<Void> validateAlgorithm(Token token) {
		String algorithm = token.getSignatureAlgorithm();
		if (algorithm == null) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.INVALID_SIGNATURE,
					"Signing algorithm cannot be null"));
		}
		if (!algorithm.equals("RS256")) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM,
					"Signing algorithm " + algorithm + " not supported"));
		}
		return Mono.empty();
	}

	/**
     * Validates the key ID and signature of a token.
     *
     * @param token The token to be validated.
     * @return A Mono that completes when the validation is successful.
     * @throws CloudFoundryAuthorizationException if the RSA signature does not match the content.
     */
    private Mono<Void> validateKeyIdAndSignature(Token token) {
		return getTokenKey(token).filter((tokenKey) -> hasValidSignature(token, tokenKey))
			.switchIfEmpty(Mono.error(new CloudFoundryAuthorizationException(Reason.INVALID_SIGNATURE,
					"RSA Signature did not match content")))
			.then();
	}

	/**
     * Retrieves the token key for the given token.
     * 
     * @param token The token for which to retrieve the key.
     * @return A Mono emitting the token key as a String.
     * @throws CloudFoundryAuthorizationException if the key ID present in the token header does not match any of the fetched token keys.
     */
    private Mono<String> getTokenKey(Token token) {
		String keyId = token.getKeyId();
		String cached = this.cachedTokenKeys.get(keyId);
		if (cached != null) {
			return Mono.just(cached);
		}
		return this.securityService.fetchTokenKeys()
			.doOnSuccess(this::cacheTokenKeys)
			.filter((tokenKeys) -> tokenKeys.containsKey(keyId))
			.map((tokenKeys) -> tokenKeys.get(keyId))
			.switchIfEmpty(Mono.error(new CloudFoundryAuthorizationException(Reason.INVALID_KEY_ID,
					"Key Id present in token header does not match")));
	}

	/**
     * Caches the provided token keys.
     * 
     * @param tokenKeys the token keys to be cached
     */
    private void cacheTokenKeys(Map<String, String> tokenKeys) {
		this.cachedTokenKeys = Map.copyOf(tokenKeys);
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
     * @param token The token to be validated.
     * @return A Mono that completes successfully if the token is not expired, or throws a CloudFoundryAuthorizationException if the token is expired.
     */
    private Mono<Void> validateExpiry(Token token) {
		long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		if (currentTime > token.getExpiry()) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.TOKEN_EXPIRED, "Token expired"));
		}
		return Mono.empty();
	}

	/**
     * Validates the issuer of a token.
     * 
     * @param token The token to be validated.
     * @return A Mono that completes when the validation is done.
     * @throws CloudFoundryAuthorizationException If the token issuer does not match.
     */
    private Mono<Void> validateIssuer(Token token) {
		return this.securityService.getUaaUrl()
			.map((uaaUrl) -> String.format("%s/oauth/token", uaaUrl))
			.filter((issuerUri) -> issuerUri.equals(token.getIssuer()))
			.switchIfEmpty(Mono
				.error(new CloudFoundryAuthorizationException(Reason.INVALID_ISSUER, "Token issuer does not match")))
			.then();
	}

	/**
     * Validates the audience of the given token.
     *
     * @param token the token to validate
     * @return a Mono that completes successfully if the audience is valid, or completes with an error if the audience is invalid
     * @throws CloudFoundryAuthorizationException if the token does not have the required audience
     */
    private Mono<Void> validateAudience(Token token) {
		if (!token.getScope().contains("actuator.read")) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.INVALID_AUDIENCE,
					"Token does not have audience actuator"));
		}
		return Mono.empty();
	}

}
