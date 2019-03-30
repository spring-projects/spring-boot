/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Token}.
 *
 * @author Madhura Bhave
 */
public class TokenTests {

	@Test
	public void invalidJwtShouldThrowException() {
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(() -> new Token("invalid-token"))
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void invalidJwtClaimsShouldThrowException() {
		String header = "{\"alg\": \"RS256\", \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "invalid-claims";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(() -> new Token(Base64Utils.encodeToString(header.getBytes())
						+ "." + Base64Utils.encodeToString(claims.getBytes())))
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void invalidJwtHeaderShouldThrowException() {
		String header = "invalid-header";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(() -> new Token(Base64Utils.encodeToString(header.getBytes())
						+ "." + Base64Utils.encodeToString(claims.getBytes())))
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void emptyJwtSignatureShouldThrowException() {
		String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ.";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(() -> new Token(token))
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void validJwt() {
		String header = "{\"alg\": \"RS256\",  \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		String content = Base64Utils.encodeToString(header.getBytes()) + "."
				+ Base64Utils.encodeToString(claims.getBytes());
		String signature = Base64Utils.encodeToString("signature".getBytes());
		Token token = new Token(content + "." + signature);
		assertThat(token.getExpiry()).isEqualTo(2147483647);
		assertThat(token.getIssuer()).isEqualTo("http://localhost:8080/uaa/oauth/token");
		assertThat(token.getSignatureAlgorithm()).isEqualTo("RS256");
		assertThat(token.getKeyId()).isEqualTo("key-id");
		assertThat(token.getContent()).isEqualTo(content.getBytes());
		assertThat(token.getSignature())
				.isEqualTo(Base64Utils.decodeFromString(signature));
	}

	@Test
	public void getSignatureAlgorithmWhenAlgIsNullShouldThrowException() {
		String header = "{\"kid\": \"key-id\",  \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		Token token = createToken(header, claims);
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(token::getSignatureAlgorithm)
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void getIssuerWhenIssIsNullShouldThrowException() {
		String header = "{\"alg\": \"RS256\", \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647}";
		Token token = createToken(header, claims);
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(token::getIssuer)
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void getKidWhenKidIsNullShouldThrowException() {
		String header = "{\"alg\": \"RS256\", \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647}";
		Token token = createToken(header, claims);
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(token::getKeyId)
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	@Test
	public void getExpiryWhenExpIsNullShouldThrowException() {
		String header = "{\"alg\": \"RS256\",  \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "{\"iss\": \"http://localhost:8080/uaa/oauth/token\"" + "}";
		Token token = createToken(header, claims);
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
				.isThrownBy(token::getExpiry)
				.satisfies(reasonRequirement(Reason.INVALID_TOKEN));
	}

	private Token createToken(String header, String claims) {
		Token token = new Token(Base64Utils.encodeToString(header.getBytes()) + "."
				+ Base64Utils.encodeToString(claims.getBytes()) + "."
				+ Base64Utils.encodeToString("signature".getBytes()));
		return token;
	}

	private Consumer<CloudFoundryAuthorizationException> reasonRequirement(
			Reason reason) {
		return (ex) -> assertThat(ex.getReason()).isEqualTo(reason);
	}

}
