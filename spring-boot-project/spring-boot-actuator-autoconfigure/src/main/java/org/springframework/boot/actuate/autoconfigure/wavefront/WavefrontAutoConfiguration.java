/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.wavefront;

import java.util.function.Supplier;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for for Wavefront common
 * infrastructure.
 *
 * @author Moritz Halbritter
 * @author Glenn Oppegard
 * @author Phillip Webb
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ApplicationTags.class, WavefrontSender.class })
@EnableConfigurationProperties(WavefrontProperties.class)
public class WavefrontAutoConfiguration {

	/**
	 * Default value for the Wavefront Service name if {@code spring.application.name} is
	 * not set.
	 * @see <a href=
	 * "https://docs.wavefront.com/trace_data_details.html#application-tags">Wavefront
	 * Application Tags</a>
	 */
	private static final String DEFAULT_SERVICE_NAME = "unnamed_service";

	/**
	 * Default value for the Wavefront Application name.
	 * @see <a href=
	 * "https://docs.wavefront.com/trace_data_details.html#application-tags">Wavefront
	 * Application Tags</a>
	 */
	private static final String DEFAULT_APPLICATION_NAME = "unnamed_application";

	@Bean
	@ConditionalOnMissingBean
	public ApplicationTags wavefrontApplicationTags(Environment environment, WavefrontProperties properties) {
		String wavefrontServiceName = getName(properties.getServiceName(),
				() -> environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME));
		String wavefrontApplicationName = getName(properties.getApplicationName(), () -> DEFAULT_APPLICATION_NAME);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		ApplicationTags.Builder builder = new ApplicationTags.Builder(wavefrontApplicationName, wavefrontServiceName);
		map.from(properties::getClusterName).to(builder::cluster);
		map.from(properties::getShardName).to(builder::shard);
		return builder.build();
	}

	private String getName(String value, Supplier<String> fallback) {
		return (StringUtils.hasText(value)) ? value : fallback.get();
	}

}
