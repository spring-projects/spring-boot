/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.writer.MessageChannelMetricWriter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for writing metrics to a
 * {@link MessageChannel}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(MessageChannel.class)
@ConditionalOnBean(name = "metricsChannel")
@AutoConfigureBefore(MetricRepositoryAutoConfiguration.class)
public class MetricsChannelAutoConfiguration {

	@Bean
	@ExportMetricWriter
	@ConditionalOnMissingBean
	public MessageChannelMetricWriter messageChannelMetricWriter(
			@Qualifier("metricsChannel") MessageChannel channel) {
		return new MessageChannelMetricWriter(channel);
	}

}
