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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AuthorizationExceptionMatcher;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TokenValidator}.
 *
 * @author Madhura Bhave
 */
public class TokenValidatorTests {

	private static final byte[] DOT = ".".getBytes();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private CloudFoundrySecurityService securityService;

	private TokenValidator tokenValidator;

	private static final String VALID_KEY = "-----BEGIN PUBLIC KEY-----\n"
			+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0m59l2u9iDnMbrXHfqkO\n"
			+ "rn2dVQ3vfBJqcDuFUK03d+1PZGbVlNCqnkpIJ8syFppW8ljnWweP7+LiWpRoz0I7\n"
			+ "fYb3d8TjhV86Y997Fl4DBrxgM6KTJOuE/uxnoDhZQ14LgOU2ckXjOzOdTsnGMKQB\n"
			+ "LCl0vpcXBtFLMaSbpv1ozi8h7DJyVZ6EnFQZUWGdgTMhDrmqevfx95U/16c5WBDO\n"
			+ "kqwIn7Glry9n9Suxygbf8g5AzpWcusZgDLIIZ7JTUldBb8qU2a0Dl4mvLZOn4wPo\n"
			+ "jfj9Cw2QICsc5+Pwf21fP+hzf+1WSRHbnYv8uanRO0gZ8ekGaghM/2H6gqJbo2nI\n"
			+ "JwIDAQAB\n-----END PUBLIC KEY-----";

	private static final String INVALID_KEY = "-----BEGIN PUBLIC KEY-----\n"
			+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxzYuc22QSst/dS7geYYK\n"
			+ "5l5kLxU0tayNdixkEQ17ix+CUcUbKIsnyftZxaCYT46rQtXgCaYRdJcbB3hmyrOa\n"
			+ "vkhTpX79xJZnQmfuamMbZBqitvscxW9zRR9tBUL6vdi/0rpoUwPMEh8+Bw7CgYR0\n"
			+ "FK0DhWYBNDfe9HKcyZEv3max8Cdq18htxjEsdYO0iwzhtKRXomBWTdhD5ykd/fAC\n"
			+ "VTr4+KEY+IeLvubHVmLUhbE5NgWXxrRpGasDqzKhCTmsa2Ysf712rl57SlH0Wz/M\n"
			+ "r3F7aM9YpErzeYLrl0GhQr9BVJxOvXcVd4kmY+XkiCcrkyS1cnghnllh+LCwQu1s\n"
			+ "YwIDAQAB\n-----END PUBLIC KEY-----";

	private static final Map<String, String> INVALID_KEYS = Collections
			.singletonMap("invalid-key", INVALID_KEY);

	private static final Map<String, String> VALID_KEYS = Collections
			.singletonMap("valid-key", VALID_KEY);

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.tokenValidator = new TokenValidator(this.securityService);
	}

	@Test
	public void validateTokenWhenKidValidationFailsTwiceShouldThrowException()
			throws Exception {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys", INVALID_KEYS);
		given(this.securityService.fetchTokenKeys()).willReturn(INVALID_KEYS);
		String header = "{\"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_KEY_ID));
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
	}

	@Test
	public void validateTokenWhenKidValidationSucceedsInTheSecondAttempt()
			throws Exception {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys", INVALID_KEYS);
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
		verify(this.securityService).fetchTokenKeys();
	}

	@Test
	public void validateTokenShouldFetchTokenKeysIfNull() throws Exception {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
		verify(this.securityService).fetchTokenKeys();
	}

	@Test
	public void validateTokenWhenValidShouldNotFetchTokenKeys() throws Exception {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys", VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
		verify(this.securityService, Mockito.never()).fetchTokenKeys();
	}

	@Test
	public void validateTokenWhenSignatureInvalidShouldThrowException() throws Exception {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys",
				Collections.singletonMap("valid-key", INVALID_KEY));
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.thrown.expect(
				AuthorizationExceptionMatcher.withReason(Reason.INVALID_SIGNATURE));
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
	}

	@Test
	public void validateTokenWhenTokenAlgorithmIsNotRS256ShouldThrowException()
			throws Exception {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		String header = "{ \"alg\": \"HS256\",  \"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.thrown.expect(AuthorizationExceptionMatcher
				.withReason(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM));
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
	}

	@Test
	public void validateTokenWhenExpiredShouldThrowException() throws Exception {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\", \"typ\": \"JWT\"}";
		String claims = "{ \"jti\": \"0236399c350c47f3ae77e67a75e75e7d\", \"exp\": 1477509977, \"scope\": [\"actuator.read\"]}";
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.TOKEN_EXPIRED));
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
	}

	@Test
	public void validateTokenWhenIssuerIsNotValidShouldThrowException() throws Exception {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://other-uaa.com");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\", \"typ\": \"JWT\", \"scope\": [\"actuator.read\"]}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_ISSUER));
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
	}

	@Test
	public void validateTokenWhenAudienceIsNotValidShouldThrowException()
			throws Exception {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\", \"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"foo.bar\"]}";
		this.thrown.expect(
				AuthorizationExceptionMatcher.withReason(Reason.INVALID_AUDIENCE));
		this.tokenValidator.validate(
				new Token(getSignedToken(header.getBytes(), claims.getBytes())));
	}

	private String getSignedToken(byte[] header, byte[] claims) throws Exception {
		PrivateKey privateKey = getPrivateKey();
		Signature signature = Signature.getInstance("SHA256WithRSA");
		signature.initSign(privateKey);
		byte[] content = dotConcat(Base64Utils.encodeUrlSafe(header),
				Base64Utils.encode(claims));
		signature.update(content);
		byte[] crypto = signature.sign();
		byte[] token = dotConcat(Base64Utils.encodeUrlSafe(header),
				Base64Utils.encodeUrlSafe(claims), Base64Utils.encodeUrlSafe(crypto));
		return new String(token, StandardCharsets.UTF_8);
	}

	private PrivateKey getPrivateKey()
			throws InvalidKeySpecException, NoSuchAlgorithmException {
		String signingKey = "-----BEGIN PRIVATE KEY-----\n"
				+ "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDSbn2Xa72IOcxu\n"
				+ "tcd+qQ6ufZ1VDe98EmpwO4VQrTd37U9kZtWU0KqeSkgnyzIWmlbyWOdbB4/v4uJa\n"
				+ "lGjPQjt9hvd3xOOFXzpj33sWXgMGvGAzopMk64T+7GegOFlDXguA5TZyReM7M51O\n"
				+ "ycYwpAEsKXS+lxcG0UsxpJum/WjOLyHsMnJVnoScVBlRYZ2BMyEOuap69/H3lT/X\n"
				+ "pzlYEM6SrAifsaWvL2f1K7HKBt/yDkDOlZy6xmAMsghnslNSV0FvypTZrQOXia8t\n"
				+ "k6fjA+iN+P0LDZAgKxzn4/B/bV8/6HN/7VZJEdudi/y5qdE7SBnx6QZqCEz/YfqC\n"
				+ "olujacgnAgMBAAECggEAc9X2tJ/OWWrXqinOg160gkELloJxTi8lAFsDbAGuAwpT\n"
				+ "JcWl1KF5CmGBjsY/8ElNi2J9GJL1HOwcBhikCVNARD1DhF6RkB13mvquWwWtTMvt\n"
				+ "eP8JWM19DIc+E+hw2rCuTGngqs7l4vTqpzBTNPtS2eiIJ1IsjsgvSEiAlk/wnW48\n"
				+ "11cf6SQMQcT3HNTWrS+yLycEuWKb6Khh8RpD9D+i8w2+IspWz5lTP7BrKCUNsLOx\n"
				+ "6+5T52HcaZ9z3wMnDqfqIKWl3h8M+q+HFQ4EN5BPWYV4fF7EOx7+Qf2fKDFPoTjC\n"
				+ "VTWzDRNAA1xPqwdF7IdPVOXCdaUJDOhHeXZGaTNSwQKBgQDxb9UiR/Jh1R3muL7I\n"
				+ "neIt1gXa0O+SK7NWYl4DkArYo7V81ztxI8r+xKEeu5zRZZkpaJHxOnd3VfADascw\n"
				+ "UfALvxGxN2z42lE6zdhrmxZ3ma+akQFsv7NyXcBT00sdW+xmOiCaAj0cgxNOXiV3\n"
				+ "sYOwUy3SqUIPO2obpb+KC5ALHwKBgQDfH+NSQ/jn89oVZ3lzUORa+Z+aL1TGsgzs\n"
				+ "p7IG0MTEYiR9/AExYUwJab0M4PDXhumeoACMfkCFALNVhpch2nXZv7X5445yRgfD\n"
				+ "ONY4WknecuA0rfCLTruNWnQ3RR+BXmd9jD/5igd9hEIawz3V+jCHvAtzI8/CZIBt\n"
				+ "AArBs5kp+QKBgQCdxwN1n6baIDemK10iJWtFoPO6h4fH8h8EeMwPb/ZmlLVpnA4Q\n"
				+ "Zd+mlkDkoJ5eiRKKaPfWuOqRZeuvj/wTq7g/NOIO+bWQ+rrSvuqLh5IrHpgPXmub\n"
				+ "8bsHJhUlspMH4KagN6ROgOAG3fGj6Qp7KdpxRCpR3KJ66czxvGNrhxre6QKBgB+s\n"
				+ "MCGiYnfSprd5G8VhyziazKwfYeJerfT+DQhopDXYVKPJnQW8cQW5C8wDNkzx6sHI\n"
				+ "pqtK1K/MnKhcVaHJmAcT7qoNQlA4Xqu4qrgPIQNBvU/dDRNJVthG6c5aspEzrG8m\n"
				+ "9IHgtRV9K8EOy/1O6YqrB9kNUVWf3JccdWpvqyNJAoGAORzJiQCOk4egbdcozDTo\n"
				+ "4Tg4qk/03qpTy5k64DxkX1nJHu8V/hsKwq9Af7Fj/iHy2Av54BLPlBaGPwMi2bzB\n"
				+ "gYjmUomvx/fqOTQks9Rc4PIMB43p6Rdj0sh+52SKPDR2eHbwsmpuQUXnAs20BPPI\n"
				+ "J/OOn5zOs8yf26os0q3+JUM=\n-----END PRIVATE KEY-----";
		String privateKey = signingKey.replace("-----BEGIN PRIVATE KEY-----\n", "");
		privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");
		byte[] pkcs8EncodedBytes = Base64.decodeBase64(privateKey);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}

	private byte[] dotConcat(byte[]... bytes) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		for (int i = 0; i < bytes.length; i++) {
			if (i > 0) {
				StreamUtils.copy(DOT, result);
			}
			StreamUtils.copy(bytes[i], result);
		}
		return result.toByteArray();
	}

}
