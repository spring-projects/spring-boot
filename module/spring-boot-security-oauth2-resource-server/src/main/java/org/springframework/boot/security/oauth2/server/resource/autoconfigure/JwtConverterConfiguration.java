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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.OnPropertyListCondition;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.CollectionUtils;

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
		PropertyMapper map = PropertyMapper.get();
		OAuth2ResourceServerProperties.Jwt jwtProperties = properties.getJwt();
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		map.from(jwtProperties::getPrincipalClaimName).to(converter::setPrincipalClaimName);
		map.from(jwtProperties).as(this::grantedAuthoritiesConverter).to(converter::setJwtGrantedAuthoritiesConverter);
		return converter;
	}

	private Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter(
			OAuth2ResourceServerProperties.Jwt properties) {
		List<String> authoritiesClaimExpressions = properties.getAuthoritiesClaimExpressions();
		if (CollectionUtils.isEmpty(authoritiesClaimExpressions)) {
			return createJwtGrantedAuthoritiesConverter(properties);
		}
		return createExpressionJwtGrantedAuthoritiesConverters(properties, authoritiesClaimExpressions);
	}

	private Converter<Jwt, Collection<GrantedAuthority>> createJwtGrantedAuthoritiesConverter(
			OAuth2ResourceServerProperties.Jwt properties) {
		PropertyMapper map = PropertyMapper.get();
		JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
		map.from(properties::getAuthorityPrefix).to(converter::setAuthorityPrefix);
		map.from(properties::getAuthoritiesClaimDelimiter).to(converter::setAuthoritiesClaimDelimiter);
		map.from(properties::getAuthoritiesClaimName).to(converter::setAuthoritiesClaimName);
		return converter;
	}

	private Converter<Jwt, Collection<GrantedAuthority>> createExpressionJwtGrantedAuthoritiesConverters(
			OAuth2ResourceServerProperties.Jwt properties, List<String> claimExpressions) {
		checkMutualExclusivity(properties);
		List<Converter<Jwt, Collection<GrantedAuthority>>> converters = new ArrayList<>();
		SpelExpressionParser parser = new SpelExpressionParser();
		for (String claimExpression : claimExpressions) {
			ExpressionJwtGrantedAuthoritiesConverter converter = new ExpressionJwtGrantedAuthoritiesConverter(
					parser.parseExpression(claimExpression));
			if (properties.getAuthorityPrefix() != null) {
				converter.setAuthorityPrefix(properties.getAuthorityPrefix());
			}
			converters.add(converter);
		}
		return (converters.size() == 1) ? converters.get(0) : new DelegatingJwtGrantedAuthoritiesConverter(converters);
	}

	private void checkMutualExclusivity(OAuth2ResourceServerProperties.Jwt properties) {
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
			entries.put("spring.security.oauth2.resourceserver.jwt.authorities-claim-expressions",
					properties.getAuthoritiesClaimExpressions());
			entries.put("spring.security.oauth2.resourceserver.jwt.authorities-claim-name",
					properties.getAuthoritiesClaimName());
		}, (value) -> !nullOrEmptyList(value));
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
			entries.put("spring.security.oauth2.resourceserver.jwt.authorities-claim-expressions",
					properties.getAuthoritiesClaimExpressions());
			entries.put("spring.security.oauth2.resourceserver.jwt.authorities-claim-delimiter",
					properties.getAuthoritiesClaimDelimiter());
		}, (value) -> !nullOrEmptyList(value));
	}

	private boolean nullOrEmptyList(Object value) {
		if (value == null) {
			return true;
		}
		return (value instanceof List<?> list) ? list.isEmpty() : false;
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

		@Conditional(OnAuthoritiesExpressionsCondition.class)
		static class OnAuthoritiesExpressions {

		}

		static class OnAuthoritiesExpressionsCondition extends OnPropertyListCondition {

			OnAuthoritiesExpressionsCondition() {
				super("spring.security.oauth2.resourceserver.jwt.authorities-claim-expressions",
						() -> ConditionMessage.forCondition("Authorities claim expressions"));
			}

		}

	}

}
