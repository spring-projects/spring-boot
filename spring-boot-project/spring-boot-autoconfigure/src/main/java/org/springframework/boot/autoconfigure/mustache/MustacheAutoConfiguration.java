/*
 * Copyright 2012-2017 the original author or authors.
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

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Collector;
import com.samskivert.mustache.Mustache.TemplateLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mustache.
 *
 * @author Dave Syer
 * @author Brian Clozel
 * @since 1.2.2
 */
@Configuration
@ConditionalOnClass(Mustache.class)
@EnableConfigurationProperties(MustacheProperties.class)
@Import({ MustacheServletWebConfiguration.class, MustacheReactiveWebConfiguration.class })
public class MustacheAutoConfiguration {

	private static final Log logger = LogFactory.getLog(MustacheAutoConfiguration.class);

	private final MustacheProperties mustache;

	private final Environment environment;

	private final ApplicationContext applicationContext;

	public MustacheAutoConfiguration(MustacheProperties mustache, Environment environment,
			ApplicationContext applicationContext) {
		this.mustache = mustache;
		this.environment = environment;
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (this.mustache.isCheckTemplateLocation()) {
			TemplateLocation location = new TemplateLocation(this.mustache.getPrefix());
			if (!location.exists(this.applicationContext)) {
				logger.warn("Cannot find template location: " + location
						+ " (please add some templates, check your Mustache "
						+ "configuration, or set spring.mustache."
						+ "check-template-location=false)");
			}
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
		loader.setCharset(this.mustache.getCharsetName());
		return loader;
	}

}
