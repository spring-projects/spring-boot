/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.springframework.boot.autoconfigure.MessageSourceAutoConfiguration.ResourceBundleCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.util.StringUtils.trimAllWhitespace;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MessageSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnMissingBean(MessageSource.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Conditional(ResourceBundleCondition.class)
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "spring.messages")
public class MessageSourceAutoConfiguration {

	private static final Resource[] NO_RESOURCES = {};

	/**
	 * Comma-separated list of basenames, each following the ResourceBundle convention.
	 * Essentially a fully-qualified classpath location. If it doesn't contain a package
	 * qualifier (such as "org.mypackage"), it will be resolved from the classpath root.
	 */
	private String basename = "messages";

	/**
	 * Message bundles encoding.
	 */
	private String encoding = "utf-8";

	/**
	 * Loaded resource bundle files cache expiration, in seconds. When set to -1, bundles
	 * are cached forever.
	 */
	private int cacheSeconds = -1;

	@Bean
	public MessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		if (StringUtils.hasText(this.basename)) {
			messageSource
					.setBasenames(commaDelimitedListToStringArray(trimAllWhitespace(this.basename)));
		}
		messageSource.setDefaultEncoding(this.encoding);
		messageSource.setCacheSeconds(this.cacheSeconds);
		return messageSource;
	}

	public String getBasename() {
		return this.basename;
	}

	public void setBasename(String basename) {
		this.basename = basename;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public int getCacheSeconds() {
		return this.cacheSeconds;
	}

	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

	protected static class ResourceBundleCondition extends SpringBootCondition {

		private static ConcurrentReferenceHashMap<String, ConditionOutcome> cache = new ConcurrentReferenceHashMap<String, ConditionOutcome>();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			String basename = context.getEnvironment().getProperty(
					"spring.messages.basename", "messages");
			ConditionOutcome outcome = cache.get(basename);
			if (outcome == null) {
				outcome = getMatchOutcomeForBasename(context, basename);
				cache.put(basename, outcome);
			}
			return outcome;
		}

		private ConditionOutcome getMatchOutcomeForBasename(ConditionContext context,
				String basename) {
			for (String name : commaDelimitedListToStringArray(trimAllWhitespace(basename))) {
				for (Resource resource : getResources(context.getClassLoader(), name)) {
					if (resource.exists()) {
						return ConditionOutcome.match("Bundle found for "
								+ "spring.messages.basename: " + name);
					}
				}
			}
			return ConditionOutcome.noMatch("No bundle found for "
					+ "spring.messages.basename: " + basename);
		}

		private Resource[] getResources(ClassLoader classLoader, String name) {
			try {
				return new SkipPatternPathMatchingResourcePatternResolver(classLoader)
						.getResources("classpath*:" + name + "*.properties");
			}
			catch (Exception ex) {
				return NO_RESOURCES;
			}
		}

	}

	/**
	 * {@link PathMatchingResourcePatternResolver} that skips well known JARs that don't
	 * contain messages.properties.
	 */
	private static class SkipPatternPathMatchingResourcePatternResolver extends
			PathMatchingResourcePatternResolver {

		private static final ClassLoader ROOT_CLASSLOADER;
		static {
			ClassLoader classLoader = null;
			try {
				classLoader = ClassLoader.getSystemClassLoader();
				while (classLoader.getParent() != null) {
					classLoader = classLoader.getParent();
				}
			}
			catch (Throwable ex) {
			}
			ROOT_CLASSLOADER = classLoader;
		}

		private static final String[] SKIPPED = { "aspectjweaver-", "hibernate-core-",
				"hsqldb-", "jackson-annotations-", "jackson-core-", "jackson-databind-",
				"javassist-", "snakeyaml-", "spring-aop-", "spring-beans-",
				"spring-boot-", "spring-boot-actuator-", "spring-boot-autoconfigure-",
				"spring-core-", "spring-context-", "spring-data-commons-",
				"spring-expression-", "spring-jdbc-", "spring-orm-", "spring-tx-",
				"spring-web-", "spring-webmvc-", "tomcat-embed-", "joda-time-",
				"hibernate-entitymanager-", "hibernate-validator-", "logback-classic-",
				"logback-core-", "thymeleaf-" };

		public SkipPatternPathMatchingResourcePatternResolver(ClassLoader classLoader) {
			super(classLoader);
		}

		@Override
		protected void addAllClassLoaderJarRoots(ClassLoader classLoader,
				Set<Resource> result) {
			if (classLoader != ROOT_CLASSLOADER) {
				super.addAllClassLoaderJarRoots(classLoader, result);
			}
		}

		@Override
		protected Set<Resource> doFindAllClassPathResources(String path)
				throws IOException {
			Set<Resource> resources = super.doFindAllClassPathResources(path);
			for (Iterator<Resource> iterator = resources.iterator(); iterator.hasNext();) {
				Resource resource = iterator.next();
				for (String skipped : SKIPPED) {
					if (resource.getFilename().startsWith(skipped)) {
						iterator.remove();
						break;
					}
				}
			}
			return resources;
		}

	}

}
