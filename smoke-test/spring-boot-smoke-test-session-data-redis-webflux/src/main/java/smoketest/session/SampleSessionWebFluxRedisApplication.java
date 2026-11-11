/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;

import static org.springframework.security.config.Customizer.withDefaults;

@SpringBootApplication
public class SampleSessionWebFluxRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleSessionWebFluxRedisApplication.class);
	}

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http.authorizeExchange((exchange) -> exchange.anyExchange().authenticated());
		http.httpBasic((basic) -> basic.securityContextRepository(new WebSessionServerSecurityContextRepository()));
		http.formLogin(withDefaults());
		return http.build();
	}

}
