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

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Abstract base class for the configuration of a Thymeleaf {@link TemplateResolver}.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractTemplateResolverConfiguration {

	private static final Log logger = LogFactory
			.getLog(AbstractTemplateResolverConfiguration.class);

	private final ThymeleafProperties properties;

	private final ApplicationContext applicationContext;

	AbstractTemplateResolverConfiguration(ThymeleafProperties properties,
			ApplicationContext applicationContext) {
		this.properties = properties;
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	public void checkTemplateLocationExists() {
		boolean checkTemplateLocation = this.properties.isCheckTemplateLocation();
		if (checkTemplateLocation) {
			TemplateLocation location = new TemplateLocation(this.properties.getPrefix());
			if (!location.exists(this.applicationContext)) {
				logger.warn("Cannot find template location: " + location
						+ " (please add some templates or check "
						+ "your Thymeleaf configuration)");
			}
		}
	}

	@Bean
	public SpringResourceTemplateResolver defaultTemplateResolver() {
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
		resolver.setApplicationContext(this.applicationContext);
		resolver.setPrefix(this.properties.getPrefix());
		resolver.setSuffix(this.properties.getSuffix());
		resolver.setTemplateMode(this.properties.getMode());
		if (this.properties.getEncoding() != null) {
			resolver.setCharacterEncoding(this.properties.getEncoding().name());
		}
		resolver.setCacheable(this.properties.isCache());
		Integer order = this.properties.getTemplateResolverOrder();
		if (order != null) {
			resolver.setOrder(order);
		}
		return resolver;
	}

}
