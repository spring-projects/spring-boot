/*
 * Copyright 2012-present the original author or authors.
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
