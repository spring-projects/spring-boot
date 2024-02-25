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

package org.springframework.boot.web.servlet.view;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

	/**
	 * Checks if the resource exists for the specified locale.
	 * @param locale the locale for which to check the resource
	 * @return true if the resource exists, false otherwise
	 * @throws Exception if an error occurs while checking the resource
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		Resource resource = getApplicationContext().getResource(getUrl());
		return (resource != null && resource.exists());
	}

	/**
	 * Renders the merged template model.
	 * @param model the model containing the data to be rendered
	 * @param request the HTTP servlet request
	 * @param response the HTTP servlet response
	 * @throws Exception if an error occurs during rendering
	 */
	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Template template = createTemplate(getApplicationContext().getResource(getUrl()));
		if (template != null) {
			template.execute(model, response.getWriter());
		}
	}

	/**
	 * Creates a template using the given resource.
	 * @param resource the resource used to create the template
	 * @return the created template
	 * @throws IOException if an I/O error occurs while reading the resource
	 */
	private Template createTemplate(Resource resource) throws IOException {
		try (Reader reader = getReader(resource)) {
			return this.compiler.compile(reader);
		}
	}

	/**
	 * Returns a Reader for the given Resource.
	 * @param resource the Resource to get the Reader for
	 * @return a Reader for the given Resource
	 * @throws IOException if an I/O error occurs while getting the Reader
	 */
	private Reader getReader(Resource resource) throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(resource.getInputStream(), this.charset);
		}
		return new InputStreamReader(resource.getInputStream());
	}

}
