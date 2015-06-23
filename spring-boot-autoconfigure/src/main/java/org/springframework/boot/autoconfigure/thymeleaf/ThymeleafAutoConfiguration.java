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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import nz.net.ultraq.thymeleaf.LayoutDialect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.extras.conditionalcomments.dialect.ConditionalCommentsDialect;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.resourceresolver.SpringResourceResourceResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import com.github.mxab.thymeleaf.extras.dataattribute.dialect.DataAttributeDialect;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Thymeleaf.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Configuration
@EnableConfigurationProperties(ThymeleafProperties.class)
@ConditionalOnClass(SpringTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ThymeleafAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(name = "defaultTemplateResolver")
	public static class DefaultTemplateResolverConfiguration {

		@Autowired
		private ThymeleafProperties properties;

		@Autowired
		private ApplicationContext applicationContext;

		@PostConstruct
		public void checkTemplateLocationExists() {
			boolean checkTemplateLocation = this.properties.isCheckTemplateLocation();
			if (checkTemplateLocation) {
				TemplateLocation location = new TemplateLocation(
						this.properties.getPrefix());
				Assert.state(location.exists(this.applicationContext),
						"Cannot find template location: " + location
								+ " (please add some templates or check "
								+ "your Thymeleaf configuration)");
			}
		}

		@Bean
		public TemplateResolver defaultTemplateResolver() {
			TemplateResolver resolver = new TemplateResolver();
			resolver.setResourceResolver(thymeleafResourceResolver());
			resolver.setPrefix(this.properties.getPrefix());
			resolver.setSuffix(this.properties.getSuffix());
			resolver.setTemplateMode(this.properties.getMode());
			resolver.setCharacterEncoding(this.properties.getEncoding());
			resolver.setCacheable(this.properties.isCache());
			return resolver;
		}

		@Bean
		public SpringResourceResourceResolver thymeleafResourceResolver() {
			return new SpringResourceResourceResolver();
		}
	}

	@Configuration
	@ConditionalOnMissingBean(SpringTemplateEngine.class)
	protected static class ThymeleafDefaultConfiguration {

		@Autowired
		private final Collection<ITemplateResolver> templateResolvers = Collections
				.emptySet();

		@Autowired(required = false)
		private final Collection<IDialect> dialects = Collections.emptySet();

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
	@ConditionalOnClass(ConditionalCommentsDialect.class)
	protected static class ThymeleafConditionalCommentsDialectConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ConditionalCommentsDialect conditionalCommentsDialect() {
			return new ConditionalCommentsDialect();
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class })
	@ConditionalOnWebApplication
	protected static class ThymeleafViewResolverConfiguration {

		@Autowired
		private ThymeleafProperties properties;

		@Autowired
		private SpringTemplateEngine templateEngine;

		@Bean
		@ConditionalOnMissingBean(name = "thymeleafViewResolver")
		@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
		public ThymeleafViewResolver thymeleafViewResolver() {
			ThymeleafViewResolver resolver = new ThymeleafViewResolver();
			resolver.setTemplateEngine(this.templateEngine);
			resolver.setCharacterEncoding(this.properties.getEncoding());
			resolver.setContentType(appendCharset(this.properties.getContentType(),
					resolver.getCharacterEncoding()));
			resolver.setExcludedViewNames(this.properties.getExcludedViewNames());
			resolver.setViewNames(this.properties.getViewNames());
			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
			return resolver;
		}

		private String appendCharset(String type, String charset) {
			if (type.contains("charset=")) {
				return type;
			}
			return type + ";charset=" + charset;
		}

	}

}
