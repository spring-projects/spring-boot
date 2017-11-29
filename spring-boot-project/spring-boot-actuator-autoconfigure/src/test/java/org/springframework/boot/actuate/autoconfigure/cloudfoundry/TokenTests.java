/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Token}.
 *
 * @author Madhura Bhave
 */
public class TokenTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void invalidJwtShouldThrowException() throws Exception {
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		new Token("invalid-token");
	}

	@Test
	public void invalidJwtClaimsShouldThrowException() throws Exception {
		String header = "{\"alg\": \"RS256\", \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "invalid-claims";
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		new Token(Base64Utils.encodeToString(header.getBytes()) + "."
				+ Base64Utils.encodeToString(claims.getBytes()));
	}

	@Test
	public void invalidJwtHeaderShouldThrowException() throws Exception {
		String header = "invalid-header";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		new Token(Base64Utils.encodeToString(header.getBytes()) + "."
				+ Base64Utils.encodeToString(claims.getBytes()));
	}

	@Test
	public void emptyJwtSignatureShouldThrowException() throws Exception {
		String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ.";
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		new Token(token);
	}

	@Test
	public void validJwt() throws Exception {
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
	public void getSignatureAlgorithmWhenAlgIsNullShouldThrowException()
			throws Exception {
		String header = "{\"kid\": \"key-id\",  \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		Token token = createToken(header, claims);
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		token.getSignatureAlgorithm();
	}

	@Test
	public void getIssuerWhenIssIsNullShouldThrowException() throws Exception {
		String header = "{\"alg\": \"RS256\", \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647}";
		Token token = createToken(header, claims);
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		token.getIssuer();
	}

	@Test
	public void getKidWhenKidIsNullShouldThrowException() throws Exception {
		String header = "{\"alg\": \"RS256\", \"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647}";
		Token token = createToken(header, claims);
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		token.getKeyId();
	}

	@Test
	public void getExpiryWhenExpIsNullShouldThrowException() throws Exception {
		String header = "{\"alg\": \"RS256\",  \"kid\": \"key-id\", \"typ\": \"JWT\"}";
		String claims = "{\"iss\": \"http://localhost:8080/uaa/oauth/token\"" + "}";
		Token token = createToken(header, claims);
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		token.getExpiry();
	}

	private Token createToken(String header, String claims) {
		Token token = new Token(Base64Utils.encodeToString(header.getBytes()) + "."
				+ Base64Utils.encodeToString(claims.getBytes()) + "."
				+ Base64Utils.encodeToString("signature".getBytes()));
		return token;
	}

}
