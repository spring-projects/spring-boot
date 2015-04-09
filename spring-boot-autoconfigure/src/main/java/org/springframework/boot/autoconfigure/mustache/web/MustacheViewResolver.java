/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;

/**
 * Spring MVC {@link ViewResolver} for Mustache.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.2.2
 */
public class MustacheViewResolver extends UrlBasedViewResolver {

	private Compiler compiler = Mustache.compiler();

	private String charset;

	public MustacheViewResolver() {
		setViewClass(MustacheView.class);
	}

	/**
	 * @param compiler the compiler to set
	 */
	public void setCompiler(Compiler compiler) {
		this.compiler = compiler;
	}

	/**
	 * @param charset the charset to set
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		Resource resource = resolveResource(viewName, locale);
		if (resource == null) {
			return null;
		}
		MustacheView view = new MustacheView(createTemplate(resource));
		view.setApplicationContext(getApplicationContext());
		view.setServletContext(getServletContext());
		return view;
	}

	private Template createTemplate(Resource resource) throws IOException {
		return this.charset == null ? this.compiler.compile(new InputStreamReader(
				resource.getInputStream())) : this.compiler
				.compile(new InputStreamReader(resource.getInputStream(), this.charset));
	}

	private Resource resolveResource(String viewName, Locale locale) {
		return resolveFromLocale(viewName, getLocale(locale));
	}

	private String getLocale(Locale locale) {
		if (locale == null) {
			return "";
		}
		LocaleEditor localeEditor = new LocaleEditor();
		localeEditor.setValue(locale);
		return "_" + localeEditor.getAsText();
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
