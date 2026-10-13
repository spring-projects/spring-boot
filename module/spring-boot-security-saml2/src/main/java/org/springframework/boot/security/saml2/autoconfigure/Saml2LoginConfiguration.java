/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.saml2.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultServletWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link SecurityFilterChain} configuration for Spring Security's relying party SAML
 * support.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDefaultServletWebSecurity
@ConditionalOnBean(RelyingPartyRegistrationRepository.class)
class Saml2LoginConfiguration {

	@Bean
	SecurityFilterChain samlSecurityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
		http.saml2Login(withDefaults());
		http.saml2Logout(withDefaults());
		return http.build();
	}

}
