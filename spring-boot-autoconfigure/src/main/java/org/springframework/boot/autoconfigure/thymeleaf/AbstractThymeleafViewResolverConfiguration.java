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

import java.util.LinkedHashMap;

import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.MimeType;

/**
 * Abstract base class for the configuration of a {@link ThymeleafViewResolver}.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractThymeleafViewResolverConfiguration {

	private final ThymeleafProperties properties;

	private final SpringTemplateEngine templateEngine;

	protected AbstractThymeleafViewResolverConfiguration(ThymeleafProperties properties,
			SpringTemplateEngine templateEngine) {
		this.properties = properties;
		this.templateEngine = templateEngine;
	}

	@Bean
	@ConditionalOnMissingBean(name = "thymeleafViewResolver")
	@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
	public ThymeleafViewResolver thymeleafViewResolver() {
		ThymeleafViewResolver resolver = new ThymeleafViewResolver();
		configureTemplateEngine(resolver, this.templateEngine);
		resolver.setCharacterEncoding(this.properties.getEncoding().name());
		resolver.setContentType(appendCharset(this.properties.getContentType(),
				resolver.getCharacterEncoding()));
		resolver.setExcludedViewNames(this.properties.getExcludedViewNames());
		resolver.setViewNames(this.properties.getViewNames());
		// This resolver acts as a fallback resolver (e.g. like a
		// InternalResourceViewResolver) so it needs to have low precedence
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
		resolver.setCache(this.properties.isCache());
		return resolver;
	}

	protected abstract void configureTemplateEngine(ThymeleafViewResolver resolver,
			SpringTemplateEngine templateEngine);

	private String appendCharset(MimeType type, String charset) {
		if (type.getCharset() != null) {
			return type.toString();
		}
		LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put("charset", charset);
		parameters.putAll(type.getParameters());
		return new MimeType(type, parameters).toString();
	}

}
