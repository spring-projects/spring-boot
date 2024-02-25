/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mustache;

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
 * @since 1.2.2
 */
@AutoConfiguration
@ConditionalOnClass(Mustache.class)
@EnableConfigurationProperties(MustacheProperties.class)
@Import({ MustacheServletWebConfiguration.class, MustacheReactiveWebConfiguration.class })
public class MustacheAutoConfiguration {

	private static final Log logger = LogFactory.getLog(MustacheAutoConfiguration.class);

	private final MustacheProperties mustache;

	private final ApplicationContext applicationContext;

	/**
     * Constructs a new MustacheAutoConfiguration with the specified MustacheProperties and ApplicationContext.
     * 
     * @param mustache the MustacheProperties to be used for configuration
     * @param applicationContext the ApplicationContext to be used for configuration
     */
    public MustacheAutoConfiguration(MustacheProperties mustache, ApplicationContext applicationContext) {
		this.mustache = mustache;
		this.applicationContext = applicationContext;
		checkTemplateLocationExists();
	}

	/**
     * Checks if the template location exists.
     * 
     * If the "checkTemplateLocation" property is set to true, it creates a TemplateLocation object using the prefix specified in the Mustache configuration. 
     * It then checks if the location exists in the ApplicationContext. If the location does not exist and the logger is enabled, it logs a warning message.
     * 
     * @see TemplateLocation
     * @see MustacheAutoConfiguration
     * @see ApplicationContext
     * @see Logger
     * 
     * @since 1.0.0
     */
    public void checkTemplateLocationExists() {
		if (this.mustache.isCheckTemplateLocation()) {
			TemplateLocation location = new TemplateLocation(this.mustache.getPrefix());
			if (!location.exists(this.applicationContext) && logger.isWarnEnabled()) {
				logger.warn("Cannot find template location: " + location
						+ " (please add some templates, check your Mustache configuration, or set spring.mustache."
						+ "check-template-location=false)");
			}
		}
	}

	/**
     * Creates a Mustache compiler bean if no other bean of the same type is present.
     * 
     * @param mustacheTemplateLoader the template loader to be used by the compiler
     * @return the Mustache compiler bean
     */
    @Bean
	@ConditionalOnMissingBean
	public Mustache.Compiler mustacheCompiler(TemplateLoader mustacheTemplateLoader) {
		return Mustache.compiler().withLoader(mustacheTemplateLoader);
	}

	/**
     * Creates a MustacheResourceTemplateLoader bean if no other bean of type TemplateLoader is present.
     * 
     * The MustacheResourceTemplateLoader is responsible for loading Mustache templates from resources.
     * It uses the prefix and suffix specified in the MustacheProperties to determine the location of the templates.
     * The charset for the templates can also be configured through the MustacheProperties.
     * 
     * @return The created MustacheResourceTemplateLoader bean.
     */
    @Bean
	@ConditionalOnMissingBean(TemplateLoader.class)
	public MustacheResourceTemplateLoader mustacheTemplateLoader() {
		MustacheResourceTemplateLoader loader = new MustacheResourceTemplateLoader(this.mustache.getPrefix(),
				this.mustache.getSuffix());
		loader.setCharset(this.mustache.getCharsetName());
		return loader;
	}

}
