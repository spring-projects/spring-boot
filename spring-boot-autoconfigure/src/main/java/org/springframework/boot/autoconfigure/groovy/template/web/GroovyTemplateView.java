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

import groovy.text.Template;

import java.io.BufferedWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * @author Dave Syer
 * 
 * @since 1.1.0
 */
public class GroovyTemplateView extends AbstractUrlBasedView {

	private final Template template;

	public GroovyTemplateView(Template template) {
		this.template = template;
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		applyContentType(response);
		this.template.make(model).writeTo(new BufferedWriter(response.getWriter()));
	}

	/**
	 * Apply this view's content type as specified in the "contentType" bean property to
	 * the given response.
	 * @param response current HTTP response
	 * @see #setContentType
	 */
	protected void applyContentType(HttpServletResponse response) {
		if (response.getContentType() == null) {
			response.setContentType(getContentType());
		}
	}

}
