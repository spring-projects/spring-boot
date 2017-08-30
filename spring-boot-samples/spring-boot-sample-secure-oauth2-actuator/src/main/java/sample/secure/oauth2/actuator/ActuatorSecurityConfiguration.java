package sample.secure.oauth2.actuator;

import org.springframework.boot.autoconfigure.security.SpringBootSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Basic auth security for actuator endpoints.
 *
 * @author Madhura Bhave
 */
@Configuration
@Order(2) // before the resource server configuration
public class ActuatorSecurityConfiguration extends WebSecurityConfigurerAdapter {

	private final SpringBootSecurity springBootSecurity;

	public ActuatorSecurityConfiguration(SpringBootSecurity springBootSecurity) {
		this.springBootSecurity = springBootSecurity;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.requestMatcher(
				this.springBootSecurity.endpointIds(SpringBootSecurity.ALL_ENDPOINTS))
				.authorizeRequests().antMatchers("/**").authenticated().and().httpBasic();
	}

}
