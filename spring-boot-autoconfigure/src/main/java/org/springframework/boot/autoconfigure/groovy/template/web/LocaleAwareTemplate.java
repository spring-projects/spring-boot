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

package org.springframework.boot.autoconfigure.groovy.template.web;

import groovy.text.markup.BaseTemplate;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;

import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * @author Dave Syer
 */
public abstract class LocaleAwareTemplate extends BaseTemplate {

	public LocaleAwareTemplate(MarkupTemplateEngine templateEngine, Map<?, ?> model,
			Map<String, String> modelTypes, TemplateConfiguration configuration) {
		super(localize(templateEngine), model, modelTypes, localize(configuration));
	}

	private static MarkupTemplateEngine localize(MarkupTemplateEngine templateEngine) {
		TemplateConfiguration templateConfiguration = templateEngine
				.getTemplateConfiguration();
		ClassLoader parent = templateEngine.getTemplateLoader().getParent();
		return new MarkupTemplateEngine(parent, localize(templateConfiguration));
	}

	private static TemplateConfiguration localize(TemplateConfiguration configuration) {
		TemplateConfiguration result = new TemplateConfiguration(configuration);
		result.setLocale(LocaleContextHolder.getLocale());
		return result;
	}

}
