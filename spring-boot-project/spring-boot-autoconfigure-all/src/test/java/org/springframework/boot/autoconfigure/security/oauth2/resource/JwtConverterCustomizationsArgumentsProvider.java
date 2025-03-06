/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * {@link ArgumentsProvider Arguments provider} supplying different Spring Boot properties
 * to customize JWT converter behavior, JWT token for conversion, expected principal name
 * and expected authorities.
 *
 * @author Yan Kardziyaka
 */
public final class JwtConverterCustomizationsArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
		String customPrefix = "CUSTOM_AUTHORITY_PREFIX_";
		String customDelimiter = "[~,#:]";
		String customAuthoritiesClaim = "custom_authorities";
		String customPrincipalClaim = "custom_principal";
		String jwkSetUriProperty = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com";
		String authorityPrefixProperty = "spring.security.oauth2.resourceserver.jwt.authority-prefix=" + customPrefix;
		String authoritiesDelimiterProperty = "spring.security.oauth2.resourceserver.jwt.authorities-claim-delimiter="
				+ customDelimiter;
		String authoritiesClaimProperty = "spring.security.oauth2.resourceserver.jwt.authorities-claim-name="
				+ customAuthoritiesClaim;
		String principalClaimProperty = "spring.security.oauth2.resourceserver.jwt.principal-claim-name="
				+ customPrincipalClaim;
		String[] customPrefixProps = { jwkSetUriProperty, authorityPrefixProperty };
		String[] customDelimiterProps = { jwkSetUriProperty, authorityPrefixProperty, authoritiesDelimiterProperty };
		String[] customAuthoritiesClaimProps = { jwkSetUriProperty, authoritiesClaimProperty };
		String[] customPrincipalClaimProps = { jwkSetUriProperty, principalClaimProperty };
		String[] allJwtConverterProps = { jwkSetUriProperty, authorityPrefixProperty, authoritiesDelimiterProperty,
				authoritiesClaimProperty, principalClaimProperty };
		String[] jwtScopes = { "custom_scope0", "custom_scope1" };
		String subjectValue = UUID.randomUUID().toString();
		String customPrincipalValue = UUID.randomUUID().toString();
		Jwt.Builder jwtBuilder = Jwt.withTokenValue("token")
			.header("alg", "none")
			.expiresAt(Instant.MAX)
			.issuedAt(Instant.MIN)
			.issuer("https://issuer.example.org")
			.jti("jti")
			.notBefore(Instant.MIN)
			.subject(subjectValue)
			.claim(customPrincipalClaim, customPrincipalValue);
		Jwt noAuthoritiesCustomizationsJwt = jwtBuilder.claim("scp", jwtScopes[0] + " " + jwtScopes[1]).build();
		Jwt customAuthoritiesDelimiterJwt = jwtBuilder.claim("scp", jwtScopes[0] + "~" + jwtScopes[1]).build();
		Jwt customAuthoritiesClaimJwt = jwtBuilder.claim("scp", null)
			.claim(customAuthoritiesClaim, jwtScopes[0] + " " + jwtScopes[1])
			.build();
		Jwt customAuthoritiesClaimAndDelimiterJwt = jwtBuilder.claim("scp", null)
			.claim(customAuthoritiesClaim, jwtScopes[0] + "~" + jwtScopes[1])
			.build();
		String[] customPrefixAuthorities = { customPrefix + jwtScopes[0], customPrefix + jwtScopes[1] };
		String[] defaultPrefixAuthorities = { "SCOPE_" + jwtScopes[0], "SCOPE_" + jwtScopes[1] };
		return Stream.of(
				Arguments.of(Named.named("Custom prefix for GrantedAuthority", customPrefixProps),
						noAuthoritiesCustomizationsJwt, subjectValue, customPrefixAuthorities),
				Arguments.of(Named.named("Custom delimiter for JWT scopes", customDelimiterProps),
						customAuthoritiesDelimiterJwt, subjectValue, customPrefixAuthorities),
				Arguments.of(Named.named("Custom JWT authority claim name", customAuthoritiesClaimProps),
						customAuthoritiesClaimJwt, subjectValue, defaultPrefixAuthorities),
				Arguments.of(Named.named("Custom JWT principal claim name", customPrincipalClaimProps),
						noAuthoritiesCustomizationsJwt, customPrincipalValue, defaultPrefixAuthorities),
				Arguments.of(Named.named("All JWT converter customizations", allJwtConverterProps),
						customAuthoritiesClaimAndDelimiterJwt, customPrincipalValue, customPrefixAuthorities));
	}

}
