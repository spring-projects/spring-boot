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

package org.springframework.boot.autoconfigure.groovy.template;

import groovy.text.SimpleTemplateEngine;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.groovy.template.web.GroovyTemplateViewResolver;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Autoconfiguration support for Groovy templates in MVC. By default creates a
 * {@link MarkupTemplateEngine} configured from {@link GroovyTemplateProperties}, but you
 * can override that by providing a {@link TemplateEngine} of a different type.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(TemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(GroovyTemplateProperties.class)
public class GroovyTemplateAutoConfiguration {

	@Autowired
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Autowired
	private GroovyTemplateProperties properties;

	public abstract static class BaseGroovyTemplateConfiguration implements
			BeanClassLoaderAware {

		@Autowired
		private GroovyTemplateProperties properties;

		@Autowired
		private ApplicationContext resourceLoader;

		private ClassLoader classLoader = GroovyWebConfiguration.class.getClassLoader();

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		protected ClassLoader createParentLoaderForTemplates() throws Exception {
			Resource[] resources = this.resourceLoader.getResources(this.properties
					.getPrefix());
			if (resources.length > 0) {
				List<URL> urls = new ArrayList<URL>();
				for (Resource resource : resources) {
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
				return new URLClassLoader(urls.toArray(new URL[urls.size()]),
						this.classLoader);
			}
			else {
				return this.classLoader;
			}
		}

	}

	@Configuration
	@ConditionalOnClass(MarkupTemplateEngine.class)
	public static class GroovyMarkupConfiguration extends BaseGroovyTemplateConfiguration {

		@Autowired
		private GroovyTemplateProperties properties;

		@Bean
		@ConfigurationProperties(prefix = "spring.groovy.template.configuration")
		public TemplateConfiguration groovyTemplateConfiguration() {
			return new TemplateConfiguration();
		}

		@Bean
		@ConditionalOnMissingBean(TemplateEngine.class)
		public TemplateEngine groovyTemplateEngine() throws Exception {
			TemplateConfiguration configuration = groovyTemplateConfiguration();
			configuration.setCacheTemplates(this.properties.isCache());
			return new MarkupTemplateEngine(createParentLoaderForTemplates(),
					configuration, new GroovyTemplateResolver());
		}

	}

	@Configuration
	@ConditionalOnMissingClass(name = "groovy.text.markup.MarkupTemplateEngine")
	public static class GroovySimpleConfiguration extends BaseGroovyTemplateConfiguration {

		@Autowired
		private GroovyTemplateProperties properties;

		@Bean
		@ConditionalOnMissingBean(TemplateEngine.class)
		public TemplateEngine groovyTemplateEngine() throws Exception {
			return new SimpleTemplateEngine(createParentLoaderForTemplates());
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, LocaleContextHolder.class,
			UrlBasedViewResolver.class })
	@ConditionalOnWebApplication
	public static class GroovyWebConfiguration {

		@Autowired
		private GroovyTemplateProperties properties;

		@Bean
		@ConditionalOnMissingBean(name = "groovyTemplateViewResolver")
		public GroovyTemplateViewResolver groovyTemplateViewResolver(TemplateEngine engine) {
			GroovyTemplateViewResolver resolver = new GroovyTemplateViewResolver();
			resolver.setPrefix(this.properties.getPrefix());
			resolver.setSuffix(this.properties.getSuffix());
			resolver.setCache(this.properties.isCache());
			resolver.setContentType(this.properties.getContentType());
			resolver.setViewNames(this.properties.getViewNames());
			resolver.setTemplateEngine(engine);

			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 6);

			return resolver;
		}

	}

}
