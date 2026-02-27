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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * {@link Configuration @Configuration} for JWT converter beans.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 * @author Yan Kardziyaka
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JwtDecoder.class)
@ConditionalOnMissingBean(JwtAuthenticationConverter.class)
@Conditional(JwtConverterConfiguration.PropertiesCondition.class)
class JwtConverterConfiguration {

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter(OAuth2ResourceServerProperties properties) {
		return jwtAuthenticationConverter(properties.getJwt());
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter(OAuth2ResourceServerProperties.Jwt properties) {
		PropertyMapper map = PropertyMapper.get();
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		map.from(properties::getPrincipalClaimName).to(converter::setPrincipalClaimName);
		map.from(properties).as(this::getGrantedAuthoritiesConverter).to(converter::setJwtGrantedAuthoritiesConverter);
		return converter;
	}

	private JwtGrantedAuthoritiesConverter getGrantedAuthoritiesConverter(
			OAuth2ResourceServerProperties.Jwt properties) {
		PropertyMapper map = PropertyMapper.get();
		JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
		map.from(properties::getAuthorityPrefix).to(converter::setAuthorityPrefix);
		map.from(properties::getAuthoritiesClaimDelimiter).to(converter::setAuthoritiesClaimDelimiter);
		map.from(properties::getAuthoritiesClaimName).to(converter::setAuthoritiesClaimName);
		return converter;
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
