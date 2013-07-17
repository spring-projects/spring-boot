/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.autoconfigure.thymeleaf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Servlet;

import nz.net.ultraq.thymeleaf.LayoutDialect;

import org.springframework.autoconfigure.AutoConfigureAfter;
import org.springframework.autoconfigure.EnableAutoConfiguration;
import org.springframework.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.bootstrap.context.condition.ConditionalOnClass;
import org.springframework.bootstrap.context.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.extras.springsecurity3.dialect.SpringSecurityDialect;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.spring3.SpringTemplateEngine;
import org.thymeleaf.spring3.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Thymeleaf templating.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(SpringTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ThymeleafAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(name = "defaultTemplateResolver")
	protected static class DefaultTemplateResolverConfiguration {

		@Autowired
		private ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Value("${spring.template.prefix:classpath:/templates/}")
		private String prefix = "classpath:/templates/";

		@Value("${spring.template.suffix:.html}")
		private String suffix = ".html";

		@Value("${spring.template.cache:true}")
		private boolean cacheable;

		@Value("${spring.template.mode:HTML5}")
		private String templateMode = "HTML5";

		@Bean
		public ITemplateResolver defaultTemplateResolver() {
			TemplateResolver resolver = new TemplateResolver();
			resolver.setResourceResolver(new IResourceResolver() {
				@Override
				public InputStream getResourceAsStream(
						TemplateProcessingParameters templateProcessingParameters,
						String resourceName) {
					try {
						return DefaultTemplateResolverConfiguration.this.resourceLoader
								.getResource(resourceName).getInputStream();
					}
					catch (IOException ex) {
						return null;
					}
				}

				@Override
				public String getName() {
					return "SPRING";
				}
			});
			resolver.setPrefix(this.prefix);
			resolver.setSuffix(this.suffix);
			resolver.setTemplateMode(this.templateMode);
			resolver.setCacheable(this.cacheable);
			return resolver;
		}

	}

	@Configuration
	@ConditionalOnMissingBean(SpringTemplateEngine.class)
	protected static class ThymeleafDefaultConfiguration {

		@Autowired
		private Collection<ITemplateResolver> templateResolvers = Collections.emptySet();

		@Autowired(required = false)
		private Collection<IDialect> dialects = Collections.emptySet();

		@Bean
		public SpringTemplateEngine templateEngine() {
			SpringTemplateEngine engine = new SpringTemplateEngine();
			for (ITemplateResolver templateResolver : this.templateResolvers) {
				engine.addTemplateResolver(templateResolver);
			}
			for (IDialect dialect : this.dialects) {
				engine.addDialect(dialect);
			}
			return engine;
		}

	}

	@Configuration
	@ConditionalOnClass(name = "nz.net.ultraq.thymeleaf.LayoutDialect")
	protected static class ThymeleafWebLayoutConfiguration {

		@Bean
		public LayoutDialect layoutDialect() {
			return new LayoutDialect();
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class })
	protected static class ThymeleafViewResolverConfiguration {

		@Autowired
		private SpringTemplateEngine templateEngine;

		@Bean
		@ConditionalOnMissingBean(name = "thymeleafViewResolver")
		public ThymeleafViewResolver thymeleafViewResolver() {
			ThymeleafViewResolver resolver = new ThymeleafViewResolver();
			resolver.setTemplateEngine(this.templateEngine);
			resolver.setCharacterEncoding("UTF-8");
			return resolver;
		}

	}

	@Configuration
	@ConditionalOnClass({ SpringSecurityDialect.class })
	protected static class ThymeleafSecurityDialectConfiguration {

		@Bean
		public SpringSecurityDialect securityDialect() {
			return new SpringSecurityDialect();
		}
	}

}
