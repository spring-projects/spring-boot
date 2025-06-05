/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.metrics.autoconfigure.ssl;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL metrics.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(after = { CompositeMeterRegistryAutoConfiguration.class, MetricsAutoConfiguration.class,
		SslAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean({ MeterRegistry.class, SslBundles.class })
public class SslMetricsAutoConfiguration {

	@Bean
	SslMeterBinder sslMeterBinder(SslBundles sslBundles, ObjectProvider<SslInfo> sslInfo) {
		return new SslMeterBinder(sslInfo.getIfAvailable(() -> new SslInfo(sslBundles)), sslBundles);
	}

}
