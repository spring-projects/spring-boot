/*
 * Copyright 2012-2023 the original author or authors.
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

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;

import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties.Application;
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
 * {@link EnableAutoConfiguration Auto-configuration} for Wavefront common infrastructure.
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

	@Bean
	@ConditionalOnMissingBean
	public ApplicationTags wavefrontApplicationTags(Environment environment, WavefrontProperties properties) {
		Application application = properties.getApplication();
		String serviceName = application.getServiceName();
		serviceName = (StringUtils.hasText(serviceName)) ? serviceName
				: environment.getProperty("spring.application.name", "unnamed_service");
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		ApplicationTags.Builder builder = new ApplicationTags.Builder(application.getName(), serviceName);
		map.from(application::getClusterName).to(builder::cluster);
		map.from(application::getShardName).to(builder::shard);
		map.from(application::getCustomTags).to(builder::customTags);
		return builder.build();
	}

}
