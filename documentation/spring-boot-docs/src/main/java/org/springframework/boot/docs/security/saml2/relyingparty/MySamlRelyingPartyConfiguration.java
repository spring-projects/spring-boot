/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.security.saml2.relyingparty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
public class MySamlRelyingPartyConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
		http.saml2Login(withDefaults());
		http.saml2Logout((saml2) -> {
			saml2.logoutRequest((request) -> request.logoutUrl("/SLOService.saml2"));
			saml2.logoutResponse((response) -> response.logoutUrl("/SLOService.saml2"));
		});
		return http.build();
	}

}
