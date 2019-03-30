/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;
import org.springframework.util.Base64Utils;

/**
 * Validator used to ensure that a signed {@link Token} has not been tampered with.
 *
 * @author Madhura Bhave
 */
class ReactiveTokenValidator {

	private final ReactiveCloudFoundrySecurityService securityService;

	private volatile ConcurrentMap<String, String> cachedTokenKeys = new ConcurrentHashMap<>();

	ReactiveTokenValidator(ReactiveCloudFoundrySecurityService securityService) {
		this.securityService = securityService;
	}

	public Mono<Void> validate(Token token) {
		return validateAlgorithm(token).then(validateKeyIdAndSignature(token))
				.then(validateExpiry(token)).then(validateIssuer(token))
				.then(validateAudience(token));
	}

	private Mono<Void> validateAlgorithm(Token token) {
		String algorithm = token.getSignatureAlgorithm();
		if (algorithm == null) {
			return Mono.error(new CloudFoundryAuthorizationException(
					Reason.INVALID_SIGNATURE, "Signing algorithm cannot be null"));
		}
		if (!algorithm.equals("RS256")) {
			return Mono.error(new CloudFoundryAuthorizationException(
					Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM,
					"Signing algorithm " + algorithm + " not supported"));
		}
		return Mono.empty();
	}

	private Mono<Void> validateKeyIdAndSignature(Token token) {
		return getTokenKey(token).filter((tokenKey) -> hasValidSignature(token, tokenKey))
				.switchIfEmpty(Mono.error(new CloudFoundryAuthorizationException(
						Reason.INVALID_SIGNATURE, "RSA Signature did not match content")))
				.then();
	}

	private Mono<String> getTokenKey(Token token) {
		String keyId = token.getKeyId();
		String cached = this.cachedTokenKeys.get(keyId);
		if (cached != null) {
			return Mono.just(cached);
		}
		return this.securityService.fetchTokenKeys().doOnSuccess(this::cacheTokenKeys)
				.filter((tokenKeys) -> tokenKeys.containsKey(keyId))
				.map((tokenKeys) -> tokenKeys.get(keyId))
				.switchIfEmpty(Mono.error(
						new CloudFoundryAuthorizationException(Reason.INVALID_KEY_ID,
								"Key Id present in token header does not match")));
	}

	private void cacheTokenKeys(Map<String, String> tokenKeys) {
		this.cachedTokenKeys = new ConcurrentHashMap<>(tokenKeys);
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

	private Mono<Void> validateExpiry(Token token) {
		long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		if (currentTime > token.getExpiry()) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.TOKEN_EXPIRED,
					"Token expired"));
		}
		return Mono.empty();
	}

	private Mono<Void> validateIssuer(Token token) {
		return this.securityService.getUaaUrl()
				.map((uaaUrl) -> String.format("%s/oauth/token", uaaUrl))
				.filter((issuerUri) -> issuerUri.equals(token.getIssuer()))
				.switchIfEmpty(Mono.error(new CloudFoundryAuthorizationException(
						Reason.INVALID_ISSUER, "Token issuer does not match")))
				.then();
	}

	private Mono<Void> validateAudience(Token token) {
		if (!token.getScope().contains("actuator.read")) {
			return Mono.error(new CloudFoundryAuthorizationException(
					Reason.INVALID_AUDIENCE, "Token does not have audience actuator"));
		}
		return Mono.empty();
	}

}
