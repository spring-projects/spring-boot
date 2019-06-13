/*
 * Copyright 2012-2019 the original author or authors.
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

package sample.security.method;

import java.util.Date;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SampleMethodSecurityApplication implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("login");
		registry.addViewController("/access").setViewName("access");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleMethodSecurityApplication.class).run(args);
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@Configuration(proxyBeanMethods = false)
	protected static class AuthenticationSecurity {

		@SuppressWarnings("deprecation")
		@Bean
		public InMemoryUserDetailsManager inMemoryUserDetailsManager() throws Exception {
			return new InMemoryUserDetailsManager(
					User.withDefaultPasswordEncoder().username("admin").password("admin")
							.roles("ADMIN", "USER", "ACTUATOR").build(),
					User.withDefaultPasswordEncoder().username("user").password("user").roles("USER").build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http.authorizeRequests()
					.antMatchers("/login").permitAll()
					.anyRequest().fullyAuthenticated()
					.and()
				.formLogin().loginPage("/login").failureUrl("/login?error")
					.and()
				.logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
					.and()
				.exceptionHandling().accessDeniedPage("/access?error");
			// @formatter:on
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Order(1)
	protected static class ActuatorSecurity extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http.requestMatcher(EndpointRequest.toAnyEndpoint()).authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.httpBasic();
			// @formatter:on
		}

	}

	@Controller
	protected static class HomeController {

		@GetMapping("/")
		@Secured("ROLE_ADMIN")
		public String home(Map<String, Object> model) {
			model.put("message", "Hello World");
			model.put("title", "Hello Home");
			model.put("date", new Date());
			return "home";
		}

	}

}
