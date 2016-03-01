/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.EnvironmentInfoProvider;
import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.boot.actuate.info.ScmGitPropertiesInfoProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for standard {@link InfoProvider}s.
 *
 * @author Meang Akira Tanaka
 * @since 1.3.0
 */
@Configuration
@AutoConfigureBefore({EndpointAutoConfiguration.class})
public class InfoProviderAutoConfiguration {

	@Autowired
	private final ConfigurableEnvironment environment = new StandardEnvironment();

	@Value("${spring.git.properties:classpath:git.properties}")
	private Resource gitProperties;

	@Bean
	@ConditionalOnMissingBean(name = "environmentInfoProvider")
	public InfoProvider environmentInfoProvider() throws Exception {
		return new EnvironmentInfoProvider(this.environment);
	}

	@Bean
	@ConditionalOnMissingBean(name = "scmInfoProvider")
	public InfoProvider scmInfoProvider() throws Exception {
		return new ScmGitPropertiesInfoProvider(this.gitProperties);
	}

}
