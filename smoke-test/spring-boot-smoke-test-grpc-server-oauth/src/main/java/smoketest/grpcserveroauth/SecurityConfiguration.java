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
