/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.reactive.result.view;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * Spring WebFlux {@link ViewResolver} for Mustache.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class MustacheViewResolver extends UrlBasedViewResolver {

	private final Compiler compiler;

	private String charset;

	/**
	 * Create a {@code MustacheViewResolver} backed by a default instance of a
	 * {@link Compiler}.
	 */
	public MustacheViewResolver() {
		this.compiler = Mustache.compiler();
		setViewClass(requiredViewClass());
	}

	/**
	 * Create a {@code MustacheViewResolver} backed by a custom instance of a
	 * {@link Compiler}.
	 * @param compiler the Mustache compiler used to compile templates
	 */
	public MustacheViewResolver(Compiler compiler) {
		this.compiler = compiler;
		setViewClass(requiredViewClass());
	}

	/**
	 * Set the charset.
	 * @param charset the charset
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	protected Class<?> requiredViewClass() {
		return MustacheView.class;
	}

	@Override
	protected AbstractUrlBasedView createView(String viewName) {
		MustacheView view = (MustacheView) super.createView(viewName);
		view.setCompiler(this.compiler);
		view.setCharset(this.charset);
		return view;
	}

}
