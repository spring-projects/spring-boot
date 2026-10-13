/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.TemplateLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mustache.
 *
 * @author Dave Syer
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Mustache.class)
@EnableConfigurationProperties(MustacheProperties.class)
@Import({ MustacheServletWebConfiguration.class, MustacheReactiveWebConfiguration.class })
public final class MustacheAutoConfiguration {

	private static final Log logger = LogFactory.getLog(MustacheAutoConfiguration.class);

	private final MustacheProperties mustache;

	private final ApplicationContext applicationContext;

	MustacheAutoConfiguration(MustacheProperties mustache, ApplicationContext applicationContext) {
		this.mustache = mustache;
		this.applicationContext = applicationContext;
		checkTemplateLocationExists();
	}

	private void checkTemplateLocationExists() {
		if (this.mustache.isCheckTemplateLocation()) {
			TemplateLocation location = new TemplateLocation(this.mustache.getPrefix());
			if (!location.exists(this.applicationContext) && logger.isWarnEnabled()) {
				logger.warn("Cannot find template location: " + location
						+ " (please add some templates, check your Mustache configuration, or set spring.mustache."
						+ "check-template-location=false)");
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	Mustache.Compiler mustacheCompiler(TemplateLoader mustacheTemplateLoader) {
		return Mustache.compiler().withLoader(mustacheTemplateLoader);
	}

	@Bean
	@ConditionalOnMissingBean(TemplateLoader.class)
	MustacheResourceTemplateLoader mustacheTemplateLoader() {
		MustacheResourceTemplateLoader loader = new MustacheResourceTemplateLoader(this.mustache.getPrefix(),
				this.mustache.getSuffix());
		loader.setCharset(this.mustache.getCharset());
		return loader;
	}

}
