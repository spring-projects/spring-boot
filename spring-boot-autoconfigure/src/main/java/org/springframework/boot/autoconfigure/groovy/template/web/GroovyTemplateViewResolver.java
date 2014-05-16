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

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;

import java.util.Locale;

import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * @author Dave Syer
 * @since 1.1.0
 */
public class GroovyTemplateViewResolver extends UrlBasedViewResolver {

	private TemplateEngine engine = new SimpleTemplateEngine();

	public GroovyTemplateViewResolver() {
		setViewClass(GroovyTemplateView.class);
	}

	/**
	 * @param engine the engine to set
	 */
	public void setTemplateEngine(TemplateEngine engine) {
		this.engine = engine;
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		Resource resource = resolveResource(viewName, locale);
		if (resource == null) {
			return null;
		}
		Template template = this.engine.createTemplate(resource.getURL());
		GroovyTemplateView view = new GroovyTemplateView(template);
		view.setApplicationContext(getApplicationContext());
		view.setServletContext(getServletContext());
		view.setContentType(getContentType());
		return view;
	}

	private Resource resolveResource(String viewName, Locale locale) {
		String l10n = "";
		if (locale != null) {
			LocaleEditor localeEditor = new LocaleEditor();
			localeEditor.setValue(locale);
			l10n = "_" + localeEditor.getAsText();
		}
		return resolveFromLocale(viewName, l10n);
	}

	private Resource resolveFromLocale(String viewName, String locale) {
		Resource resource = getApplicationContext().getResource(
				getPrefix() + viewName + locale + getSuffix());
		if (resource == null || !resource.exists()) {
			if (locale.isEmpty()) {
				return null;
			}
			int index = locale.lastIndexOf("_");
			return resolveFromLocale(viewName, locale.substring(0, index));
		}
		return resource;
	}

}
