/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * {@link SpringBootServletInitializer} for CLI packaged WAR files.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class SpringApplicationWebApplicationInitializer
		extends SpringBootServletInitializer {

	/**
	 * The entry containing the source class.
	 */
	public static final String SOURCE_ENTRY = "Spring-Application-Source-Classes";

	private String[] sources;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		try {
			this.sources = getSources(servletContext);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		super.onStartup(servletContext);
	}

	private String[] getSources(ServletContext servletContext) throws IOException {
		Manifest manifest = getManifest(servletContext);
		if (manifest == null) {
			throw new IllegalStateException("Unable to read manifest");
		}
		String sources = manifest.getMainAttributes().getValue(SOURCE_ENTRY);
		return sources.split(",");
	}

	private Manifest getManifest(ServletContext servletContext) throws IOException {
		InputStream stream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
		return (stream != null) ? new Manifest(stream) : null;
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Class<?>[] sourceClasses = new Class<?>[this.sources.length];
			for (int i = 0; i < this.sources.length; i++) {
				sourceClasses[i] = classLoader.loadClass(this.sources[i]);
			}
			return builder.sources(sourceClasses)
					.properties("spring.groovy.template.check-template-location=false");
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
