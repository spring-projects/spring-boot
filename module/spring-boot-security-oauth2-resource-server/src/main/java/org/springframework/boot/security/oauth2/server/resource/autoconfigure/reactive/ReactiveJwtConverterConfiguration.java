/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;

/**
 * {@link Configuration @Configuration} for JWT converter beans.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Anastasiia Losieva
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 * @author Yan Kardziyaka
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ReactiveJwtDecoder.class)
@ConditionalOnMissingBean(ReactiveJwtAuthenticationConverter.class)
@Conditional(ReactiveJwtConverterConfiguration.PropertiesCondition.class)
class ReactiveJwtConverterConfiguration {

	@Bean
	ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter(OAuth2ResourceServerProperties properties) {
		return reactiveJwtAuthenticationConverter(properties.getJwt());
	}

	private ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter(
			OAuth2ResourceServerProperties.Jwt properties1) {
		PropertyMapper map = PropertyMapper.get();
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		map.from(properties1.getAuthorityPrefix()).to(grantedAuthoritiesConverter::setAuthorityPrefix);
		map.from(properties1.getAuthoritiesClaimDelimiter())
			.to(grantedAuthoritiesConverter::setAuthoritiesClaimDelimiter);
		map.from(properties1.getAuthoritiesClaimName()).to(grantedAuthoritiesConverter::setAuthoritiesClaimName);
		ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
		map.from(properties1.getPrincipalClaimName()).to(jwtAuthenticationConverter::setPrincipalClaimName);
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
				new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter));
		return jwtAuthenticationConverter;
	}

	static class PropertiesCondition extends AnyNestedCondition {

		PropertiesCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.authority-prefix")
		static class OnAuthorityPrefix {

		}

		@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.principal-claim-name")
		static class OnPrincipalClaimName {

		}

		@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.authorities-claim-name")
		static class OnAuthoritiesClaimName {

		}

	}

}
