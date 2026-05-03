/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.mustache.reactive.view;

import java.nio.charset.Charset;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import org.jspecify.annotations.Nullable;

import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * Spring WebFlux {@link ViewResolver} for Mustache.
 *
 * @author Brian Clozel
 * @author Marten Deinum
 * @since 4.0.0
 */
public class MustacheViewResolver extends UrlBasedViewResolver {

	private final Compiler compiler;

	private @Nullable Charset charset;

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
	 * Set the {@link Charset} to use.
	 * @param charset the charset
	 * @since 4.1.0
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	/**
	 * Set the name of the charset to use.
	 * @param charset the charset
	 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
	 * {@link #setCharset(Charset)}
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public void setCharset(@Nullable String charset) {
		setCharset((charset != null) ? Charset.forName(charset) : null);
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

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == MustacheView.class) ? new MustacheView() : super.instantiateView();
	}

}
