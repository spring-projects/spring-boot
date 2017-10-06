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

package org.springframework.boot.web.servlet.view;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * Spring MVC {@link View} using the Mustache template engine.
 *
 * @author Brian Clozel
 * @author Dave Syer
 * @author Phillip Webb
 * @since 2.0.0
 */
public class MustacheView extends AbstractTemplateView {

	private Compiler compiler;

	private String charset;

	/**
	 * Set the Mustache compiler to be used by this view.
	 * <p>
	 * Typically this property is not set directly. Instead a single {@link Compiler} is
	 * expected in the Spring application context which is used to compile Mustache
	 * templates.
	 * @param compiler the Mustache compiler
	 */
	public void setCompiler(Compiler compiler) {
		this.compiler = compiler;
	}

	/**
	 * Set the charset used for reading Mustache template files.
	 * @param charset the charset to use for reading template files
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	public boolean checkResource(Locale locale) throws Exception {
		Resource resource = getApplicationContext().getResource(this.getUrl());
		return (resource != null && resource.exists());
	}

	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		Template template = createTemplate(
				getApplicationContext().getResource(this.getUrl()));
		if (template != null) {
			template.execute(model, response.getWriter());
		}
	}

	private Template createTemplate(Resource resource) throws IOException {
		try (Reader reader = getReader(resource)) {
			return this.compiler.compile(reader);
		}
	}

	private Reader getReader(Resource resource) throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(resource.getInputStream(), this.charset);
		}
		return new InputStreamReader(resource.getInputStream());
	}

}
