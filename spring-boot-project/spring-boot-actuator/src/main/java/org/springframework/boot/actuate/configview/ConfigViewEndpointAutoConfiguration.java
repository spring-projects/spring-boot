package org.springframework.boot.actuate.autoconfigure.configview;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.configview.ConfigViewEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailableEndpoint(endpoint = ConfigViewEndpoint.class)
public class ConfigViewEndpointAutoConfiguration {

	@Bean
	public ConfigViewEndpoint configViewEndpoint(ConfigurableEnvironment environment) {
		return new ConfigViewEndpoint(environment);
	}
}
