/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure.jdbc;

import javax.sql.DataSource;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class SampleWebSecureJdbcApplication implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/login").setViewName("login");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleWebSecureJdbcApplication.class).run(args);
	}

	@Configuration(proxyBeanMethods = false)
	protected static class ApplicationSecurity {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) {
			http.csrf(CsrfConfigurer::disable);
			http.authorizeHttpRequests((requests) -> {
				requests.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll();
				requests.anyRequest().fullyAuthenticated();
			});
			http.formLogin((form) -> form.loginPage("/login").permitAll());
			return http.build();
		}

		@Bean
		public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
			return new JdbcUserDetailsManager(dataSource);
		}

	}

}
