package org.springframework.boot.actuate.configview;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigViewEndpointTests {

	@Test
	void returnsAllConfigProperties() {
		ConfigurableEnvironment env = new StandardEnvironment();
		Map<String, Object> props = new HashMap<>();
		props.put("my.custom.key", "value");
		props.put("spring.datasource.password", "supersecret");
		env.getPropertySources().addFirst(new MapPropertySource("test", props));

		ConfigViewEndpoint endpoint = new ConfigViewEndpoint(env);
		List<ConfigViewEndpoint.ConfigProperty> result = endpoint.configProperties();

		assertThat(result).anySatisfy(prop -> {
			assertThat(prop.key()).isEqualTo("my.custom.key");
			assertThat(prop.value()).isEqualTo("value");
			assertThat(prop.source()).isEqualTo("test");
		});

		assertThat(result).anySatisfy(prop -> {
			assertThat(prop.key()).isEqualTo("spring.datasource.password");
			assertThat(prop.value()).isEqualTo("****");
		});
	}
}
