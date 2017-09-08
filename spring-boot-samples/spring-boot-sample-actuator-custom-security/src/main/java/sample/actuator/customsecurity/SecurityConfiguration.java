package sample.actuator.customsecurity;

import org.springframework.boot.autoconfigure.security.SpringBootSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	private SpringBootSecurity bootSecurity;

	public SecurityConfiguration(SpringBootSecurity bootSecurity) {
		this.bootSecurity = bootSecurity;
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser("user").password("password")
				.authorities("ROLE_USER").and().withUser("admin").password("admin")
				.authorities("ROLE_ACTUATOR", "ROLE_USER");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http.authorizeRequests()
				.requestMatchers(this.bootSecurity.endpointIds("status", "info")).permitAll()
				.requestMatchers(this.bootSecurity.endpointIds(SpringBootSecurity.ALL_ENDPOINTS)).hasRole("ACTUATOR")
				.requestMatchers(this.bootSecurity.staticResources()).permitAll()
				.antMatchers("/foo").permitAll()
				.antMatchers("/**").hasRole("USER")
				.and()
			.cors()
				.and()
			.httpBasic();
		// @formatter:on
	}

}
