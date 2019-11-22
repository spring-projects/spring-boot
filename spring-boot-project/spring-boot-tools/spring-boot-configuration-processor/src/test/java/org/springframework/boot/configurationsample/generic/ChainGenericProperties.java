package org.springframework.boot.configurationsample.generic;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;

/**
 * Chain Generic Properties
 *
 * @author L.cm
 */
@ConfigurationProperties("generic")
public class ChainGenericProperties {

	@NestedConfigurationProperty
	private ChainGenericConfig config;

	public ChainGenericConfig getConfig() {
		return config;
	}

	public void setConfig(ChainGenericConfig config) {
		this.config = config;
	}
}
