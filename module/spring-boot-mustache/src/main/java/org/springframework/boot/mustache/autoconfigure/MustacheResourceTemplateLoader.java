/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import java.io.InputStreamReader;
import java.io.Reader;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Mustache.TemplateLoader;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Mustache TemplateLoader implementation that uses a prefix, suffix and the Spring
 * Resource abstraction to load a template from a file, classpath, URL etc. A
 * {@link TemplateLoader} is needed in the {@link Compiler} when you want to render
 * partials (i.e. tiles-like features).
 *
 * @author Dave Syer
 * @since 4.0.0
 * @see Mustache
 * @see Resource
 */
public class MustacheResourceTemplateLoader implements TemplateLoader, ResourceLoaderAware {

	private String prefix = "";

	private String suffix = "";

	private String charSet = "UTF-8";

	private ResourceLoader resourceLoader = new DefaultResourceLoader(null);

	public MustacheResourceTemplateLoader() {
	}

	public MustacheResourceTemplateLoader(String prefix, String suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}

	/**
	 * Set the charset.
	 * @param charSet the charset
	 */
	public void setCharset(String charSet) {
		this.charSet = charSet;
	}

	/**
	 * Set the resource loader.
	 * @param resourceLoader the resource loader
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Reader getTemplate(String name) throws Exception {
		return new InputStreamReader(this.resourceLoader.getResource(this.prefix + name + this.suffix).getInputStream(),
				this.charSet);
	}

}
