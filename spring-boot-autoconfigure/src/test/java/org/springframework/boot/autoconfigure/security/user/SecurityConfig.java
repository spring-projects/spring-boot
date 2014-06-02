
package org.springframework.boot.autoconfigure.security.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
@EntityScan(basePackageClasses = User.class)
@EnableJpaRepositories(basePackageClasses = User.class)
public class SecurityConfig extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	private UserRepository userRepository;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		System.err.println("Global security config");
	}

}
