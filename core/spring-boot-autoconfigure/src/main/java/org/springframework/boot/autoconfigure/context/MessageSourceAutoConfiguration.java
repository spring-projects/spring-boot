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

package org.springframework.boot.autoconfigure.context;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.MessageSourceRuntimeHints;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MessageSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Marc Becker
 * @author Misagh Moayyed
 * @author Henrique (henriquejsza)
 * @since 1.5.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(MessageSourceProperties.class)
@ImportRuntimeHints(MessageSourceRuntimeHints.class)
public final class MessageSourceAutoConfiguration {

	private static final Resource[] NO_RESOURCES = {};

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	ResourceBasedMessageSourceConfigurer resourceBasedMessageSourceConfigurer(MessageSourceProperties properties) {
		return new ResourceBasedMessageSourceConfigurer(properties);
	}

	@Bean
	@ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME,
			search = SearchStrategy.CURRENT)
	@Conditional(ResourceBundleCondition.class)
	ResourceBundleMessageSource messageSource(ResourceBasedMessageSourceConfigurer configurer) {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		configurer.configure(messageSource);
		return messageSource;
	}

	protected static class ResourceBundleCondition extends SpringBootCondition {

		private static final ConcurrentReferenceHashMap<String, ConditionOutcome> cache = new ConcurrentReferenceHashMap<>();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String basename = context.getEnvironment().getProperty("spring.messages.basename", "messages");
			ConditionOutcome outcome = cache.get(basename);
			if (outcome == null) {
				outcome = getMatchOutcomeForBasename(context, basename);
				cache.put(basename, outcome);
			}
			return outcome;
		}

		private ConditionOutcome getMatchOutcomeForBasename(ConditionContext context, String basename) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("ResourceBundle");
			for (String name : StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(basename))) {
				for (Resource resource : getResources(context.getClassLoader(), name)) {
					if (resource.exists()) {
						return ConditionOutcome.match(message.found("bundle").items(resource));
					}
				}
			}
			return ConditionOutcome.noMatch(message.didNotFind("bundle with basename " + basename).atAll());
		}

		private Resource[] getResources(@Nullable ClassLoader classLoader, String name) {
			String target = name.replace('.', '/');
			try {
				return new PathMatchingResourcePatternResolver(classLoader)
					.getResources("classpath*:" + target + ".properties");
			}
			catch (Exception ex) {
				return NO_RESOURCES;
			}
		}

	}

	static class MessageSourceRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.resources().registerPattern("messages.properties").registerPattern("messages_*.properties");
		}

	}

}
