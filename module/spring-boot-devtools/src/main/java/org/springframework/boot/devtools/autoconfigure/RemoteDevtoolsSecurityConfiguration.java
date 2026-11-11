/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Spring Security configuration that allows anonymous access to the remote devtools
 * endpoint.
 *
 * @author Madhura Bhave
 */
@ConditionalOnClass({ SecurityFilterChain.class, HttpSecurity.class })
@Configuration(proxyBeanMethods = false)
class RemoteDevtoolsSecurityConfiguration {

	private final String url;

	RemoteDevtoolsSecurityConfiguration(DevToolsProperties devToolsProperties, ServerProperties serverProperties) {
		ServerProperties.Servlet servlet = serverProperties.getServlet();
		String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
		this.url = servletContextPath + devToolsProperties.getRemote().getContextPath() + "/restart";
	}

	@Bean
	@Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
	SecurityFilterChain devtoolsSecurityFilterChain(HttpSecurity http) {
		http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(this.url));
		http.authorizeHttpRequests((requests) -> requests.anyRequest().anonymous());
		http.csrf(CsrfConfigurer::disable);
		return http.build();
	}

}
