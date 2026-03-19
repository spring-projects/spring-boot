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

package smoketest.grpcserversecure;

import io.grpc.ServerInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.RequestMapperConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	@Bean
	InMemoryUserDetailsManager inMemoryUserDetailsManager() {
		UserDetails user = user("user", "{noop}userpassword", "ROLE_USER");
		UserDetails admin = user("admin", "{noop}adminpassword", "ROLE_ADMIN");
		return new InMemoryUserDetailsManager(user, admin);
	}

	private UserDetails user(String username, String password, String authority) {
		return User.withUsername(username).password(password).authorities(authority).build();
	}

	@Bean
	@GlobalServerInterceptor
	ServerInterceptor securityInterceptor(GrpcSecurity security) throws Exception {
		return security.authorizeRequests(this::authorizeRequests)
			.httpBasic(withDefaults())
			.preauth(withDefaults())
			.build();
	}

	private void authorizeRequests(RequestMapperConfigurer requests) {
		requests.methods("HelloWorld/SayHelloAdmin").hasAuthority("ROLE_ADMIN");
		requests.methods("HelloWorld/SayHelloAdminAnnotated").hasAuthority("ROLE_ADMIN");
		requests.methods("HelloWorld/SayHelloUser").hasAuthority("ROLE_USER");
		requests.methods("HelloWorld/SayHelloUserAnnotated").hasAuthority("ROLE_USER");
		requests.methods("grpc.*/*").permitAll();
		requests.allRequests().denyAll();
	}

}
