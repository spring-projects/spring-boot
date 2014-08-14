/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.hdiv;

import org.hdiv.config.annotation.ExclusionRegistry;
import org.hdiv.config.annotation.configuration.HdivWebSecurityConfigurerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleHdivApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleHdivApplication.class, args);
	}

	@Bean
	public WebConfig webConfig() {
		return new WebConfig();
	}
	
	protected static class WebConfig extends WebMvcConfigurerAdapter{
		
		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/login").setViewName("login");
		}
	}
	
	@Bean
	public ApplicationSecurity applicationSecurity() {
		return new ApplicationSecurity();
	}

	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {
		
		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
	        auth
	            .inMemoryAuthentication()
	                .withUser("david").password("david").roles("USER","ADMIN").and()
	                .withUser("alex").password("alex").roles("USER").and()
	                .withUser("tim").password("tim").roles("USER");
		}
		
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.authorizeRequests().anyRequest().fullyAuthenticated().and()
				.formLogin()
					.loginPage("/login")
					.failureUrl("/login?error").permitAll();
		}
	}
	
	@Bean
	public ApplicationWebSecurity applicationWebSecurity() {
		return new ApplicationWebSecurity();
	}

	protected static class ApplicationWebSecurity extends HdivWebSecurityConfigurerAdapter {

		@Override
		public void addExclusions(ExclusionRegistry registry) {
			registry.addUrlExclusions("/login");
		}
	}

}
