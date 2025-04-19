package org.springframework.boot.actuate.autoconfigure.configfile;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("management.endpoint.configfile")
public class ConfigFileEndpointProperties {

	private boolean enabled = false;

	private List<String> sensitiveKeys = List.of("password", "secret", "token");

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getSensitiveKeys() {
		return sensitiveKeys;
	}

	public void setSensitiveKeys(List<String> sensitiveKeys) {
		this.sensitiveKeys = sensitiveKeys;
	}
}
