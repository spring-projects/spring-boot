/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.scan.valid;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration;

/**
 * Used for testing {@link ConfigurationProperties @ConfigurationProperties} scanning.
 *
 * @author Madhura Bhave
 */
@ConfigurationPropertiesScan
public class ConfigurationPropertiesScanConfiguration {

	@ConfigurationPropertiesScan
	@EnableConfigurationProperties({ ConfigurationPropertiesScanConfiguration.FooProperties.class })
	public static class TestConfiguration {

	}

	@ConfigurationPropertiesScan(basePackages = "org.springframework.boot.context.properties.scan.valid.a",
			basePackageClasses = BScanConfiguration.class)
	public static class DifferentPackageConfiguration {

	}

	@ConfigurationProperties("foo")
	static class FooProperties {

	}

	@ConfigurationProperties("bar")
	static class BarProperties {

		BarProperties(String foo) {
		}

	}

	@ConfigurationProperties("bing")
	static class BingProperties {

		BingProperties() {
		}

		BingProperties(String foo) {
		}

	}

}
