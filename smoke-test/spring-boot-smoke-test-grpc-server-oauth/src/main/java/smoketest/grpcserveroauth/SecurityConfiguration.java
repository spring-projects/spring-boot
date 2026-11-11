/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcserveroauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.RequestMapperConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	@Bean
	@GlobalServerInterceptor
	AuthenticationProcessInterceptor securityInterceptor(GrpcSecurity grpcSecurity) throws Exception {
		return grpcSecurity.authorizeRequests(this::authorizeGrpcRequests)
			.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()))
			.build();
	}

	private void authorizeGrpcRequests(RequestMapperConfigurer requests) {
		requests.methods("HelloWorld/SayHelloProfileScope").hasAuthority("SCOPE_profile");
		requests.methods("HelloWorld/SayHelloEmailScope").hasAuthority("SCOPE_email");
		requests.methods("HelloWorld/SayHelloAuthenticated").authenticated();
		requests.methods("grpc.*/*").permitAll();
		requests.allRequests().denyAll();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http.oauth2AuthorizationServer((authorizationServer) -> {
			http.securityMatcher(authorizationServer.getEndpointsMatcher());
			authorizationServer.oidc(withDefaults());
		});
		http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
		http.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll());
		http.csrf((csrf) -> csrf.disable());
		return http.build();
	}

}
