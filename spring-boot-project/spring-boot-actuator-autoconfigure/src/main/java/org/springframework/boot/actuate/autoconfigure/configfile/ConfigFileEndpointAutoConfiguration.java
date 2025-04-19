package org.springframework.boot.actuate.autoconfigure.configfile;

import org.springframework.boot.actuate.configfile.ConfigFileEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConfigFileEndpointProperties.class)
@ConditionalOnProperty(prefix = "management.endpoint.configfile", name = "enabled", havingValue = "true")
public class ConfigFileEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	public ConfigFileEndpoint configFileEndpoint(Environment environment, ConfigFileEndpointProperties props) {
		return new ConfigFileEndpoint(environment, new java.util.HashSet<>(props.getSensitiveKeys()));
	}
}
