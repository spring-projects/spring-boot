/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

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
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.ResourceBundleCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.CollectionFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MessageSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Marc Becker
 * @author Misagh Moayyed
 * @author Daniel Lykov
 * @since 1.5.0
 */
@AutoConfiguration
@ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Conditional(ResourceBundleCondition.class)
@EnableConfigurationProperties(MessageSourceProperties.class)
@ImportRuntimeHints(MessageSourceRuntimeHints.class)
public final class MessageSourceAutoConfiguration {

	private static final Resource[] NO_RESOURCES = {};

	@Bean
	MessageSource messageSource(MessageSourceProperties properties) {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		if (!CollectionUtils.isEmpty(properties.getBasename())) {
			messageSource.setBasenames(properties.getBasename().toArray(new String[0]));
		}
		if (properties.getEncoding() != null) {
			messageSource.setDefaultEncoding(properties.getEncoding().name());
		}
		messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
		Duration cacheDuration = properties.getCacheDuration();
		if (cacheDuration != null) {
			messageSource.setCacheMillis(cacheDuration.toMillis());
		}
		messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
		messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
		messageSource.setCommonMessages(loadCommonMessages(properties.getCommonMessages()));
		return messageSource;
	}

	private @Nullable Properties loadCommonMessages(@Nullable List<Resource> resources) {
		if (CollectionUtils.isEmpty(resources)) {
			return null;
		}
		Properties properties = CollectionFactory.createSortedProperties(false);
		for (Resource resource : resources) {
			try {
				PropertiesLoaderUtils.fillProperties(properties, resource);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to load common messages from '%s'".formatted(resource), ex);
			}
		}
		return properties;
	}

	protected static class ResourceBundleCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ClassLoader classLoader = context.getClassLoader();
			String basename = context.getEnvironment().getProperty("spring.messages.basename", "messages");
			return Cache.forClassLoader(classLoader)
				.computeIfAbsent(basename, () -> getMatchOutcomeForBasename(classLoader, basename));
		}

		private ConditionOutcome getMatchOutcomeForBasename(@Nullable ClassLoader classLoader, String basename) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("ResourceBundle");
			for (String name : StringUtils.tokenizeToStringArray(basename, ",")) {
				for (Resource resource : getResources(classLoader, name)) {
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

	static final class Cache {

		private static final Cache none = new Cache(null);

		private static final Map<ClassLoader, Cache> classloaderCaches = new ConcurrentReferenceHashMap<>(16,
				ReferenceType.SOFT);

		private final @Nullable Map<String, ConditionOutcome> outcomes;

		private Cache(@Nullable Map<String, ConditionOutcome> outcomes) {
			this.outcomes = outcomes;
		}

		ConditionOutcome computeIfAbsent(String basename, Supplier<ConditionOutcome> action) {
			return (this.outcomes != null) ? this.outcomes.computeIfAbsent(basename, (key) -> action.get())
					: action.get();
		}

		static Cache forClassLoader(@Nullable ClassLoader classLoader) {
			return (classLoader != null) ? classloaderCaches.computeIfAbsent(classLoader,
					(key) -> new Cache(new ConcurrentReferenceHashMap<>())) : none;
		}

	}

	static class MessageSourceRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.resources().registerPattern("messages.properties").registerPattern("messages_*.properties");
		}

	}

}
