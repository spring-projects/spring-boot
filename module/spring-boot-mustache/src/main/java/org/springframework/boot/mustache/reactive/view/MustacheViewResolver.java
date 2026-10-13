/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.reactive.view;

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

	private @Nullable String charset;

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

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == MustacheView.class) ? new MustacheView() : super.instantiateView();
	}

}
