/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.web.secure.custom;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SampleWebSecureCustomApplication class.
 */
@SpringBootApplication
public class SampleWebSecureCustomApplication implements WebMvcConfigurer {

	/**
     * Adds view controllers to the registry.
     * 
     * @param registry the ViewControllerRegistry to add the view controllers to
     */
    @Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/login").setViewName("login");
	}

	/**
     * The main method is the entry point of the application.
     * It initializes and runs the Spring Boot application using the SpringApplicationBuilder.
     * 
     * @param args the command line arguments passed to the application
     */
    public static void main(String[] args) {
		new SpringApplicationBuilder(SampleWebSecureCustomApplication.class).run(args);
	}

	/**
     * ApplicationSecurity class.
     */
    @Configuration(proxyBeanMethods = false)
	protected static class ApplicationSecurity {

		/**
         * Configures the security filter chain for the application.
         * Disables CSRF protection.
         * Permits all requests with dispatcher type FORWARD.
         * Requires full authentication for all other requests.
         * Configures form login with a login page at "/login" and permits all access to the login page.
         * 
         * @param http the HttpSecurity object to configure
         * @return the configured SecurityFilterChain
         * @throws Exception if an error occurs during configuration
         */
        @Bean
		SecurityFilterChain configure(HttpSecurity http) throws Exception {
			http.csrf((csrf) -> csrf.disable());
			http.authorizeHttpRequests((requests) -> {
				requests.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll();
				requests.anyRequest().fullyAuthenticated();
			});
			http.formLogin((form) -> form.loginPage("/login").permitAll());
			return http.build();
		}

	}

}
