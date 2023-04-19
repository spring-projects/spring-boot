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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link TokenValidator}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class TokenValidatorTests {

	private static final byte[] DOT = ".".getBytes();

	@Mock
	private CloudFoundrySecurityService securityService;

	private TokenValidator tokenValidator;

	private static final String VALID_KEY = """
			-----BEGIN PUBLIC KEY-----
			MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0m59l2u9iDnMbrXHfqkO
			rn2dVQ3vfBJqcDuFUK03d+1PZGbVlNCqnkpIJ8syFppW8ljnWweP7+LiWpRoz0I7
			fYb3d8TjhV86Y997Fl4DBrxgM6KTJOuE/uxnoDhZQ14LgOU2ckXjOzOdTsnGMKQB
			LCl0vpcXBtFLMaSbpv1ozi8h7DJyVZ6EnFQZUWGdgTMhDrmqevfx95U/16c5WBDO
			kqwIn7Glry9n9Suxygbf8g5AzpWcusZgDLIIZ7JTUldBb8qU2a0Dl4mvLZOn4wPo
			jfj9Cw2QICsc5+Pwf21fP+hzf+1WSRHbnYv8uanRO0gZ8ekGaghM/2H6gqJbo2nI
			JwIDAQAB
			-----END PUBLIC KEY-----""";

	private static final String INVALID_KEY = """
			-----BEGIN PUBLIC KEY-----
			MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxzYuc22QSst/dS7geYYK
			5l5kLxU0tayNdixkEQ17ix+CUcUbKIsnyftZxaCYT46rQtXgCaYRdJcbB3hmyrOa
			vkhTpX79xJZnQmfuamMbZBqitvscxW9zRR9tBUL6vdi/0rpoUwPMEh8+Bw7CgYR0
			FK0DhWYBNDfe9HKcyZEv3max8Cdq18htxjEsdYO0iwzhtKRXomBWTdhD5ykd/fAC
			VTr4+KEY+IeLvubHVmLUhbE5NgWXxrRpGasDqzKhCTmsa2Ysf712rl57SlH0Wz/M
			r3F7aM9YpErzeYLrl0GhQr9BVJxOvXcVd4kmY+XkiCcrkyS1cnghnllh+LCwQu1s
			YwIDAQAB
			-----END PUBLIC KEY-----""";

	private static final Map<String, String> INVALID_KEYS = Collections.singletonMap("invalid-key", INVALID_KEY);

	private static final Map<String, String> VALID_KEYS = Collections.singletonMap("valid-key", VALID_KEY);

	@BeforeEach
	void setup() {
		this.tokenValidator = new TokenValidator(this.securityService);
	}

	@Test
	void validateTokenWhenKidValidationFailsTwiceShouldThrowException() {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys", INVALID_KEYS);
		given(this.securityService.fetchTokenKeys()).willReturn(INVALID_KEYS);
		String header = "{\"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{\"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
			.isThrownBy(
					() -> this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes()))))
			.satisfies(reasonRequirement(Reason.INVALID_KEY_ID));
	}

	@Test
	void validateTokenWhenKidValidationSucceedsInTheSecondAttempt() throws Exception {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys", INVALID_KEYS);
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes())));
		then(this.securityService).should().fetchTokenKeys();
	}

	@Test
	void validateTokenShouldFetchTokenKeysIfNull() throws Exception {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes())));
		then(this.securityService).should().fetchTokenKeys();
	}

	@Test
	void validateTokenWhenValidShouldNotFetchTokenKeys() throws Exception {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys", VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes())));
		then(this.securityService).should(never()).fetchTokenKeys();
	}

	@Test
	void validateTokenWhenSignatureInvalidShouldThrowException() {
		ReflectionTestUtils.setField(this.tokenValidator, "tokenKeys",
				Collections.singletonMap("valid-key", INVALID_KEY));
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\",\"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
			.isThrownBy(
					() -> this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes()))))
			.satisfies(reasonRequirement(Reason.INVALID_SIGNATURE));
	}

	@Test
	void validateTokenWhenTokenAlgorithmIsNotRS256ShouldThrowException() {
		String header = "{ \"alg\": \"HS256\",  \"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"actuator.read\"]}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
			.isThrownBy(
					() -> this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes()))))
			.satisfies(reasonRequirement(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM));
	}

	@Test
	void validateTokenWhenExpiredShouldThrowException() {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\", \"typ\": \"JWT\"}";
		String claims = "{ \"jti\": \"0236399c350c47f3ae77e67a75e75e7d\", \"exp\": 1477509977, \"scope\": [\"actuator.read\"]}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
			.isThrownBy(
					() -> this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes()))))
			.satisfies(reasonRequirement(Reason.TOKEN_EXPIRED));
	}

	@Test
	void validateTokenWhenIssuerIsNotValidShouldThrowException() {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("https://other-uaa.com");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\", \"typ\": \"JWT\", \"scope\": [\"actuator.read\"]}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\"}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
			.isThrownBy(
					() -> this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes()))))
			.satisfies(reasonRequirement(Reason.INVALID_ISSUER));
	}

	@Test
	void validateTokenWhenAudienceIsNotValidShouldThrowException() {
		given(this.securityService.fetchTokenKeys()).willReturn(VALID_KEYS);
		given(this.securityService.getUaaUrl()).willReturn("http://localhost:8080/uaa");
		String header = "{ \"alg\": \"RS256\",  \"kid\": \"valid-key\", \"typ\": \"JWT\"}";
		String claims = "{ \"exp\": 2147483647, \"iss\": \"http://localhost:8080/uaa/oauth/token\", \"scope\": [\"foo.bar\"]}";
		assertThatExceptionOfType(CloudFoundryAuthorizationException.class)
			.isThrownBy(
					() -> this.tokenValidator.validate(new Token(getSignedToken(header.getBytes(), claims.getBytes()))))
			.satisfies(reasonRequirement(Reason.INVALID_AUDIENCE));
	}

	private String getSignedToken(byte[] header, byte[] claims) throws Exception {
		PrivateKey privateKey = getPrivateKey();
		Signature signature = Signature.getInstance("SHA256WithRSA");
		signature.initSign(privateKey);
		byte[] content = dotConcat(Base64.getUrlEncoder().encode(header), Base64.getEncoder().encode(claims));
		signature.update(content);
		byte[] crypto = signature.sign();
		byte[] token = dotConcat(Base64.getUrlEncoder().encode(header), Base64.getUrlEncoder().encode(claims),
				Base64.getUrlEncoder().encode(crypto));
		return new String(token, StandardCharsets.UTF_8);
	}

	private PrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
		String signingKey = """
				-----BEGIN PRIVATE KEY-----
				MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDSbn2Xa72IOcxu
				tcd+qQ6ufZ1VDe98EmpwO4VQrTd37U9kZtWU0KqeSkgnyzIWmlbyWOdbB4/v4uJa
				lGjPQjt9hvd3xOOFXzpj33sWXgMGvGAzopMk64T+7GegOFlDXguA5TZyReM7M51O
				ycYwpAEsKXS+lxcG0UsxpJum/WjOLyHsMnJVnoScVBlRYZ2BMyEOuap69/H3lT/X
				pzlYEM6SrAifsaWvL2f1K7HKBt/yDkDOlZy6xmAMsghnslNSV0FvypTZrQOXia8t
				k6fjA+iN+P0LDZAgKxzn4/B/bV8/6HN/7VZJEdudi/y5qdE7SBnx6QZqCEz/YfqC
				olujacgnAgMBAAECggEAc9X2tJ/OWWrXqinOg160gkELloJxTi8lAFsDbAGuAwpT
				JcWl1KF5CmGBjsY/8ElNi2J9GJL1HOwcBhikCVNARD1DhF6RkB13mvquWwWtTMvt
				eP8JWM19DIc+E+hw2rCuTGngqs7l4vTqpzBTNPtS2eiIJ1IsjsgvSEiAlk/wnW48
				11cf6SQMQcT3HNTWrS+yLycEuWKb6Khh8RpD9D+i8w2+IspWz5lTP7BrKCUNsLOx
				6+5T52HcaZ9z3wMnDqfqIKWl3h8M+q+HFQ4EN5BPWYV4fF7EOx7+Qf2fKDFPoTjC
				VTWzDRNAA1xPqwdF7IdPVOXCdaUJDOhHeXZGaTNSwQKBgQDxb9UiR/Jh1R3muL7I
				neIt1gXa0O+SK7NWYl4DkArYo7V81ztxI8r+xKEeu5zRZZkpaJHxOnd3VfADascw
				UfALvxGxN2z42lE6zdhrmxZ3ma+akQFsv7NyXcBT00sdW+xmOiCaAj0cgxNOXiV3
				sYOwUy3SqUIPO2obpb+KC5ALHwKBgQDfH+NSQ/jn89oVZ3lzUORa+Z+aL1TGsgzs
				p7IG0MTEYiR9/AExYUwJab0M4PDXhumeoACMfkCFALNVhpch2nXZv7X5445yRgfD
				ONY4WknecuA0rfCLTruNWnQ3RR+BXmd9jD/5igd9hEIawz3V+jCHvAtzI8/CZIBt
				AArBs5kp+QKBgQCdxwN1n6baIDemK10iJWtFoPO6h4fH8h8EeMwPb/ZmlLVpnA4Q
				Zd+mlkDkoJ5eiRKKaPfWuOqRZeuvj/wTq7g/NOIO+bWQ+rrSvuqLh5IrHpgPXmub
				8bsHJhUlspMH4KagN6ROgOAG3fGj6Qp7KdpxRCpR3KJ66czxvGNrhxre6QKBgB+s
				MCGiYnfSprd5G8VhyziazKwfYeJerfT+DQhopDXYVKPJnQW8cQW5C8wDNkzx6sHI
				pqtK1K/MnKhcVaHJmAcT7qoNQlA4Xqu4qrgPIQNBvU/dDRNJVthG6c5aspEzrG8m
				9IHgtRV9K8EOy/1O6YqrB9kNUVWf3JccdWpvqyNJAoGAORzJiQCOk4egbdcozDTo
				4Tg4qk/03qpTy5k64DxkX1nJHu8V/hsKwq9Af7Fj/iHy2Av54BLPlBaGPwMi2bzB
				gYjmUomvx/fqOTQks9Rc4PIMB43p6Rdj0sh+52SKPDR2eHbwsmpuQUXnAs20BPPI
				J/OOn5zOs8yf26os0q3+JUM=
				-----END PRIVATE KEY-----""";
		String privateKey = signingKey.replace("-----BEGIN PRIVATE KEY-----\n", "");
		privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");
		privateKey = privateKey.replace("\n", "");
		byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKey);
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

	private Consumer<CloudFoundryAuthorizationException> reasonRequirement(Reason reason) {
		return (ex) -> assertThat(ex.getReason()).isEqualTo(reason);
	}

}
