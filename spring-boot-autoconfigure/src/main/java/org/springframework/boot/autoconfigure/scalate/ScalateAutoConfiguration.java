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

package org.springframework.boot.autoconfigure.scalate;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.fusesource.scalate.layout.DefaultLayoutStrategy;
import org.fusesource.scalate.servlet.Config;
import org.fusesource.scalate.servlet.ServletTemplateEngine;
import org.fusesource.scalate.spring.view.ScalateViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ServletContextAware;

import scala.collection.JavaConversions;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Scalate. Possible options by
 * application properties:
 * <ul>
 * <li>spring.scalate.prefix: The path of the templates (default: templates/)</li>
 * <li>spring.scalate.suffix: The extension of the template files. If empty, needs to be
 * specified in the controller (default: .scaml)</li>
 * <li>spring.scalate.layout: The path for the default layout (default: templates/layout.scaml)</li>
 * </ul>
 * 
 * @author Christoph Nagel
 */
@Configuration
@ConditionalOnClass(ServletTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ScalateAutoConfiguration {

	public static final String DEFAULT_PREFIX = "templates/";

	public static final String DEFAULT_SUFFIX = ".scaml";

	public static final String DEFAULT_LAYOUT = "templates/layout.scaml";

	@Configuration
	@ConditionalOnMissingBean(ServletTemplateEngine.class)
	protected static class ScalateDefaultConfiguration implements EnvironmentAware,
			ServletContextAware {

		private RelaxedPropertyResolver env;

		private ServletContext servletContext;

		@Override
		public void setEnvironment(Environment environment) {
			this.env = new RelaxedPropertyResolver(environment, "spring.scalate.");
		}

		@Override
		public void setServletContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Bean
		public Config config() {
			return new Config() {

				@Override
				public ServletContext getServletContext() {
					return servletContext;
				}

				@Override
				public String getName() {
					return env.getProperty("servlet_name", "unknown");
				}

				@Override
				public Enumeration<?> getInitParameterNames() {
					return null;
				}

				@Override
				public String getInitParameter(String name) {
					return null;
				}
			};
		}

		@Bean
		public ServletTemplateEngine servletTemplateEngine() {
			// initialize the template engine
			ServletTemplateEngine engine = new ServletTemplateEngine(config());
			// set layout strategy
			List<String> layouts = new ArrayList<String>(1);
			layouts.add(env.getProperty("layout", DEFAULT_LAYOUT));
			engine.layoutStrategy_$eq(new DefaultLayoutStrategy(engine,
					JavaConversions.asScalaBuffer(layouts)));
			return engine;
		}
	}

	@Configuration
	@ConditionalOnClass({ Servlet.class })
	protected static class ScalateViewResolverConfiguration implements EnvironmentAware {

		@Autowired
		private ServletTemplateEngine servletTemplateEngine;

		private RelaxedPropertyResolver env;

		@Override
		public void setEnvironment(Environment environment) {
			this.env = new RelaxedPropertyResolver(environment, "spring.scalate.");
		}

		@Bean
		@ConditionalOnMissingBean(name = "scalateViewResolver")
		public ScalateViewResolver scalateViewResolver() {
			ScalateViewResolver resolver = new ScalateViewResolver();
			resolver.templateEngine_$eq(servletTemplateEngine);
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 20);
			resolver.setPrefix(env.getProperty("prefix", DEFAULT_PREFIX));
			resolver.setSuffix(env.getProperty("suffix", DEFAULT_SUFFIX));
			return resolver;
		}

	}

}
