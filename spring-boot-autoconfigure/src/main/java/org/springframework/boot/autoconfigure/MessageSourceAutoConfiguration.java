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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.util.StringUtils.trimAllWhitespace;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MessageSource}.
 *
 * @author Dave Syer
 */
@Configuration
@ConditionalOnMissingBean(MessageSource.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Conditional(ResourceBundleCondition.class)
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "spring.messages")
public class MessageSourceAutoConfiguration {

	private static final Resource[] NO_RESOURCES = {};

	private String basename = "messages";

	private String encoding = "utf-8";

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

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			String basename = context.getEnvironment().getProperty(
					"spring.messages.basename", "messages");
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
				return new ExtendedPathMatchingResourcePatternResolver(classLoader)
						.getResources("classpath*:" + name + "*.properties");
			}
			catch (IOException ex) {
				return NO_RESOURCES;
			}
		}

	}

	/**
	 * Extended version of {@link PathMatchingResourcePatternResolver} to deal with the
	 * fact that "{@code classpath*:...*.properties}" patterns don't work with
	 * {@link URLClassLoader}s.
	 */
	private static class ExtendedPathMatchingResourcePatternResolver extends
			PathMatchingResourcePatternResolver {

		private static final Log logger = LogFactory
				.getLog(PathMatchingResourcePatternResolver.class);

		public ExtendedPathMatchingResourcePatternResolver(ClassLoader classLoader) {
			super(classLoader);
		}

		@Override
		protected Resource[] findAllClassPathResources(String location)
				throws IOException {
			String path = location;
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if ("".equals(path)) {
				Set<Resource> result = new LinkedHashSet<Resource>(16);
				result.addAll(Arrays.asList(super.findAllClassPathResources(location)));
				addAllClassLoaderJarUrls(getClassLoader(), result);
				return result.toArray(new Resource[result.size()]);
			}
			return super.findAllClassPathResources(location);
		}

		private void addAllClassLoaderJarUrls(ClassLoader classLoader,
				Set<Resource> result) {
			if (classLoader != null) {
				if (classLoader instanceof URLClassLoader) {
					addAllClassLoaderJarUrls(((URLClassLoader) classLoader).getURLs(),
							result);
				}
				addAllClassLoaderJarUrls(classLoader.getParent(), result);
			}
		}

		private void addAllClassLoaderJarUrls(URL[] urls, Set<Resource> result) {
			for (URL url : urls) {
				if ("file".equals(url.getProtocol())
						&& url.toString().toLowerCase().endsWith(".jar")) {
					try {
						URL jarUrl = new URL("jar:" + url.toString() + "!/");
						jarUrl.openConnection();
						result.add(convertClassLoaderURL(jarUrl));
					}
					catch (Exception ex) {
						if (logger.isWarnEnabled()) {
							logger.warn("Cannot search for matching files underneath "
									+ url + " because it cannot be accessed as a JAR", ex);
						}
					}
				}
			}
		}

	}

}
