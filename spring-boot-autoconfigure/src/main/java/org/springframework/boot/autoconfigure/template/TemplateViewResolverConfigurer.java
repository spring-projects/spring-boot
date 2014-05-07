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

package org.springframework.boot.autoconfigure.template;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * Helper class for use by configuration classes that provide an
 * {@link AbstractTemplateViewResolver} bean.
 *
 * @author Andy Wilkinson
 */
public class TemplateViewResolverConfigurer {

	/**
	 * Configures the {@code resolver} using the given {@code properties} and defaults.
	 *
	 * @param resolver The resolver to configure
	 * @param properties The properties to use to configure the resolver
	 */
	public void configureTemplateViewResolver(AbstractTemplateViewResolver resolver,
			AbstractTemplateViewResolverProperties properties) {

		resolver.setPrefix(properties.getPrefix());
		resolver.setSuffix(properties.getSuffix());
		resolver.setCache(properties.isCache());
		resolver.setContentType(properties.getContentType());
		resolver.setViewNames(properties.getViewNames());
		resolver.setExposeRequestAttributes(properties.isExposeRequestAttributes());
		resolver.setAllowRequestOverride(properties.isAllowRequestOverride());
		resolver.setExposeSessionAttributes(properties.isExposeSessionAttributes());
		resolver.setExposeSpringMacroHelpers(properties.isExposeSpringMacroHelpers());
		resolver.setRequestContextAttribute(properties.getRequestContextAttribute());

		// This resolver acts as a fallback resolver (e.g. like a
		// InternalResourceViewResolver) so it needs to have low precedence
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
	}

}
