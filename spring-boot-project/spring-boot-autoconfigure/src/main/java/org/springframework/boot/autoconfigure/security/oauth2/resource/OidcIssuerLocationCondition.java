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
package org.springframework.boot.autoconfigure.security.oauth2.resource;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Condition for creating {@link JwtDecoder} by oidc issuer location.
 *
 * @author Artsiom Yudovin
 */
public class OidcIssuerLocationCondition implements Condition {

	private static final String OIDC_ISSUER_LOCATION = "spring.security.oauth2.resourceserver.jwt.oidc-issuer-location";

	private static final String JWT_SET_URI = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";

	@Override
	public boolean matches(ConditionContext conditionContext,
			AnnotatedTypeMetadata annotatedTypeMetadata) {
		Environment environment = conditionContext.getEnvironment();
		return environment.containsProperty(OIDC_ISSUER_LOCATION)
				&& !environment.containsProperty(JWT_SET_URI);
	}

}
