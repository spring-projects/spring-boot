/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.context.properties.scan;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.scan.b.BScanConfiguration;

/**
 * Used for testing {@link ConfigurationProperties} scanning.
 *
 * @author Madhura Bhave
 */
@ConfigurationPropertiesScan
public class ConfigurationPropertiesScanConfiguration {

	@ConfigurationPropertiesScan
	@EnableConfigurationProperties({
			ConfigurationPropertiesScanConfiguration.FooProperties.class })
	public static class TestConfiguration {

	}

	@ConfigurationPropertiesScan(basePackages = "org.springframework.boot.context.properties.scan.a", basePackageClasses = BScanConfiguration.class)
	public static class DifferentPackageConfiguration {

	}

	@ConfigurationProperties(prefix = "foo")
	static class FooProperties {

	}

	@ConfigurationProperties(prefix = "bar")
	public static class BarProperties {

		public BarProperties(String foo) {

		}

	}

	@ConfigurationProperties(prefix = "bing")
	public static class BingProperties {

		public BingProperties() {

		}

		public BingProperties(String foo) {

		}

	}

}
