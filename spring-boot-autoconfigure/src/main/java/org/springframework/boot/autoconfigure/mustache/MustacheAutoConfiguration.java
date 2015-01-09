/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.mustache.web.MustacheViewResolver;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Collector;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Mustache.TemplateLoader;

/**
 * @author Dave Syer
 * @since 1.2.2
 *
 */
@Configuration
@ConditionalOnClass(Mustache.class)
@EnableConfigurationProperties(MustacheProperties.class)
public class MustacheAutoConfiguration {

	@Autowired
	private MustacheProperties mustache;

	@Autowired
	private Environment environment;

	@Autowired
	private ApplicationContext applicationContext;

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (this.mustache.isCheckTemplateLocation()) {
			TemplateLocation location = new TemplateLocation(this.mustache.getPrefix());
			Assert.state(location.exists(this.applicationContext),
					"Cannot find template location: " + location
							+ " (please add some templates, check your Mustache "
							+ "configuration, or set spring.mustache.template."
							+ "check-template-location=false)");
		}
	}

	@Bean
	@ConditionalOnMissingBean(Mustache.Compiler.class)
	public Mustache.Compiler mustacheCompiler(TemplateLoader mustacheTemplateLoader) {
		return Mustache.compiler().withLoader(mustacheTemplateLoader)
				.withCollector(collector());
	}

	private Collector collector() {
		MustacheEnvironmentCollector collector = new MustacheEnvironmentCollector();
		collector.setEnvironment(this.environment);
		return collector;
	}

	@Bean
	@ConditionalOnMissingBean(TemplateLoader.class)
	public MustacheResourceTemplateLoader mustacheTemplateLoader() {
		MustacheResourceTemplateLoader loader = new MustacheResourceTemplateLoader(
				this.mustache.getPrefix(), this.mustache.getSuffix());
		loader.setCharset(this.mustache.getCharset());
		return loader;
	}

	@Configuration
	@ConditionalOnWebApplication
	protected static class MustacheWebConfiguration {

		@Autowired
		private MustacheProperties mustache;

		@Bean
		@ConditionalOnMissingBean(MustacheViewResolver.class)
		public MustacheViewResolver mustacheViewResolver(Compiler mustacheCompiler) {
			MustacheViewResolver resolver = new MustacheViewResolver();
			resolver.setPrefix(this.mustache.getPrefix());
			resolver.setSuffix(this.mustache.getSuffix());
			resolver.setCache(this.mustache.isCache());
			resolver.setViewNames(this.mustache.getViewNames());
			resolver.setContentType(this.mustache.getContentType());
			resolver.setCompiler(mustacheCompiler);
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
			return resolver;
		}

	}
}
