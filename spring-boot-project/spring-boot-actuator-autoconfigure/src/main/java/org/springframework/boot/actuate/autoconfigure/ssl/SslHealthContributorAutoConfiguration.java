/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.ssl;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.ssl.SslHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
@AutoConfiguration(before = HealthContributorAutoConfiguration.class)
@ConditionalOnEnabledHealthIndicator("ssl")
@EnableConfigurationProperties(SslHealthIndicatorProperties.class)
public class SslHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "sslHealthIndicator")
	SslHealthIndicator sslHealthIndicator(SslInfo sslInfo) {
		return new SslHealthIndicator(sslInfo);
	}

	@Bean
	@ConditionalOnMissingBean
	SslInfo sslInfo(SslBundles sslBundles, SslHealthIndicatorProperties sslHealthIndicatorProperties) {
		return new SslInfo(sslBundles, sslHealthIndicatorProperties.getCertificateValidityWarningThreshold());
	}

}
