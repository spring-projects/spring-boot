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

package org.springframework.boot.autoconfigure.jet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.config.ConfigProvider;
import com.hazelcast.jet.impl.config.XmlJetConfigBuilder;
import com.hazelcast.jet.impl.config.YamlJetConfigBuilder;
import com.hazelcast.spring.context.SpringManagedContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hazelcast Jet. Creates a
 * {@link JetInstance} based on explicit configuration or when a default configuration
 * file is found in the environment.
 *
 * @author Ali Gurbuz
 * @since 2.3.0
 * @see HazelcastJetConfigResourceCondition
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HazelcastJetProperties.class)
@ConditionalOnMissingBean(JetInstance.class)
public class HazelcastJetServerConfiguration {

	public static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.jet.config";

	public static final String CONFIG_ENVIRONMENT_PROPERTY = "spring.hazelcast.jet.config";

	private static JetConfig getJetConfig(Resource configLocation) throws IOException {
		URL configUrl = configLocation.getURL();
		String configFileName = configUrl.getPath();
		InputStream inputStream = configUrl.openStream();
		if (configFileName.endsWith(".yaml") || configFileName.endsWith("yml")) {
			return new YamlJetConfigBuilder(inputStream).build();
		}
		return new XmlJetConfigBuilder(inputStream).build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JetConfig.class)
	@Conditional(JetConfigAvailableCondition.class)
	static class HazelcastJetServerConfigFileConfiguration {

		@Autowired
		private ApplicationContext applicationContext;

		@Bean
		JetInstance jetInstance(HazelcastJetProperties properties) throws IOException {
			Resource configLocation = properties.resolveConfigLocation();
			JetConfig jetConfig = (configLocation != null) ? getJetConfig(configLocation)
					: ConfigProvider.locateAndGetJetConfig();
			injectSpringManagedContext(jetConfig);
			return Jet.newJetInstance(jetConfig);
		}

		private void injectSpringManagedContext(JetConfig jetConfig) {
			SpringManagedContext springManagedContext = new SpringManagedContext();
			springManagedContext.setApplicationContext(this.applicationContext);
			jetConfig.getHazelcastConfig().setManagedContext(springManagedContext);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(JetConfig.class)
	static class HazelcastJetServerConfigConfiguration {

		@Bean
		JetInstance jetInstance(JetConfig jetConfig) {
			return Jet.newJetInstance(jetConfig);
		}

	}

	/**
	 * {@link HazelcastJetConfigResourceCondition} that checks if the
	 * {@code spring.hazelcast.jet.config} configuration key is defined.
	 */
	static class JetConfigAvailableCondition extends HazelcastJetConfigResourceCondition {

		JetConfigAvailableCondition() {
			super("HazelcastJet", CONFIG_ENVIRONMENT_PROPERTY, CONFIG_SYSTEM_PROPERTY, "file:./hazelcast-jet.xml",
					"classpath:/hazelcast-jet.xml", "file:./hazelcast-jet.yaml", "classpath:/hazelcast-jet.yaml",
					"file:./hazelcast-jet.yml", "classpath:/hazelcast-jet.yml");
		}

		@Override
		protected ConditionOutcome getResourceOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (System.getProperty(HazelcastJetClientConfiguration.CONFIG_SYSTEM_PROPERTY) != null) {
				return ConditionOutcome.noMatch(startConditionMessage().because(
						"System property '" + HazelcastJetClientConfiguration.CONFIG_SYSTEM_PROPERTY + "' is set."));
			}
			if (context.getEnvironment()
					.containsProperty(HazelcastJetClientConfiguration.CONFIG_ENVIRONMENT_PROPERTY)) {
				return ConditionOutcome.noMatch(startConditionMessage().because("Environment property '"
						+ HazelcastJetClientConfiguration.CONFIG_ENVIRONMENT_PROPERTY + "' is set."));
			}
			return super.getResourceOutcome(context, metadata);
		}

	}

}
