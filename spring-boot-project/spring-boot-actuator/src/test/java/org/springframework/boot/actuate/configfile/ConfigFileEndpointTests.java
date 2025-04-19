package org.springframework.boot.actuate.configfile;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFileEndpointTests {

	@Test
	void sensitiveKeysAreRedacted() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("db.password", "supersecret");
		env.setProperty("api.key", "abc123");

		ConfigFileEndpoint endpoint = new ConfigFileEndpoint(env, Set.of("password", "key"));
		Map<String, Object> config = endpoint.config();

		assertThat(config.get("db.password")).isEqualTo("****");
		assertThat(config.get("api.key")).isEqualTo("****");
	}
}
