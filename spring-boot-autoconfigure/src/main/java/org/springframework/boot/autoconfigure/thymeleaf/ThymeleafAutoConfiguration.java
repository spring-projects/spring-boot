/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.servlet.Servlet;

import com.github.mxab.thymeleaf.extras.dataattribute.dialect.DataAttributeDialect;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.extras.conditionalcomments.dialect.ConditionalCommentsDialect;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.resourceresolver.SpringResourceResourceResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Thymeleaf.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Eddú Meléndez
 */
@Configuration
@EnableConfigurationProperties(ThymeleafProperties.class)
@ConditionalOnClass(SpringTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ThymeleafAutoConfiguration {

	@Configuration
	@ConditionalOnMissingClass("org.thymeleaf.templatemode.TemplateMode")
	static class Thymeleaf2Configuration {

		@Configuration
		@ConditionalOnMissingBean(name = "defaultTemplateResolver")
		static class DefaultTemplateResolverConfiguration
				extends AbstractTemplateResolverConfiguration {

			DefaultTemplateResolverConfiguration(ThymeleafProperties properties,
					ApplicationContext applicationContext) {
				super(properties, applicationContext);
			}

			@Bean
			public SpringResourceResourceResolver thymeleafResourceResolver() {
				return new SpringResourceResourceResolver();
			}
		}

		@Configuration
		@ConditionalOnClass({ Servlet.class })
		@ConditionalOnWebApplication
		static class Thymeleaf2ViewResolverConfiguration
				extends AbstractThymeleafViewResolverConfiguration {

			Thymeleaf2ViewResolverConfiguration(ThymeleafProperties properties,
					SpringTemplateEngine templateEngine) {
				super(properties, templateEngine);
			}

			@Override
			protected void configureTemplateEngine(ThymeleafViewResolver resolver,
					SpringTemplateEngine templateEngine) {
				resolver.setTemplateEngine(templateEngine);
			}

		}

		@Configuration
		@ConditionalOnClass(ConditionalCommentsDialect.class)
		static class ThymeleafConditionalCommentsDialectConfiguration {

			@Bean
			@ConditionalOnMissingBean
			public ConditionalCommentsDialect conditionalCommentsDialect() {
				return new ConditionalCommentsDialect();
			}

		}

	}

	@Configuration
	@ConditionalOnClass(name = "org.thymeleaf.templatemode.TemplateMode")
	static class Thymeleaf3Configuration {

		@Configuration
		@ConditionalOnMissingBean(name = "defaultTemplateResolver")
		static class DefaultTemplateResolverConfiguration
				extends AbstractTemplateResolverConfiguration {

			DefaultTemplateResolverConfiguration(ThymeleafProperties properties,
					ApplicationContext applicationContext) {
				super(properties, applicationContext);
			}

		}

		@Configuration
		@ConditionalOnClass({ Servlet.class })
		@ConditionalOnWebApplication
		static class Thymeleaf3ViewResolverConfiguration
				extends AbstractThymeleafViewResolverConfiguration {

			Thymeleaf3ViewResolverConfiguration(ThymeleafProperties properties,
					SpringTemplateEngine templateEngine) {
				super(properties, templateEngine);
			}

			@Override
			protected void configureTemplateEngine(ThymeleafViewResolver resolver,
					SpringTemplateEngine templateEngine) {
				Method setTemplateEngine;
				try {
					setTemplateEngine = ReflectionUtils.findMethod(resolver.getClass(),
							"setTemplateEngine",
							Class.forName("org.thymeleaf.ITemplateEngine", true,
									resolver.getClass().getClassLoader()));
				}
				catch (ClassNotFoundException ex) {
					throw new IllegalStateException(ex);
				}
				ReflectionUtils.invokeMethod(setTemplateEngine, resolver, templateEngine);
			}

		}

	}

	@Configuration
	@ConditionalOnMissingBean(SpringTemplateEngine.class)
	protected static class ThymeleafDefaultConfiguration {

		private final Collection<ITemplateResolver> templateResolvers;

		private final Collection<IDialect> dialects;

		public ThymeleafDefaultConfiguration(
				Collection<ITemplateResolver> templateResolvers,
				ObjectProvider<Collection<IDialect>> dialectsProvider) {
			this.templateResolvers = templateResolvers;
			this.dialects = dialectsProvider.getIfAvailable();
		}

		@Bean
		public SpringTemplateEngine templateEngine() {
			SpringTemplateEngine engine = new SpringTemplateEngine();
			for (ITemplateResolver templateResolver : this.templateResolvers) {
				engine.addTemplateResolver(templateResolver);
			}
			if (!CollectionUtils.isEmpty(this.dialects)) {
				for (IDialect dialect : this.dialects) {
					engine.addDialect(dialect);
				}
			}
			return engine;
		}

	}

	@Configuration
	@ConditionalOnClass(name = "nz.net.ultraq.thymeleaf.LayoutDialect")
	protected static class ThymeleafWebLayoutConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public LayoutDialect layoutDialect() {
			return new LayoutDialect();
		}

	}

	@Configuration
	@ConditionalOnClass(DataAttributeDialect.class)
	protected static class DataAttributeDialectConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DataAttributeDialect dialect() {
			return new DataAttributeDialect();
		}

	}

	@Configuration
	@ConditionalOnClass({ SpringSecurityDialect.class })
	protected static class ThymeleafSecurityDialectConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public SpringSecurityDialect securityDialect() {
			return new SpringSecurityDialect();
		}

	}

	@Configuration
	@ConditionalOnJava(ConditionalOnJava.JavaVersion.EIGHT)
	@ConditionalOnClass(Java8TimeDialect.class)
	protected static class ThymeleafJava8TimeDialect {

		@Bean
		@ConditionalOnMissingBean
		public Java8TimeDialect java8TimeDialect() {
			return new Java8TimeDialect();
		}

	}

	@Configuration
	@ConditionalOnWebApplication
	protected static class ThymeleafResourceHandlingConfig {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledResourceChain
		public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
			return new ResourceUrlEncodingFilter();
		}

	}

}
