/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.server.ServerWebExchange;

/**
 * Spring WebFlux {@link View} using the Mustache template engine.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class MustacheView extends AbstractUrlBasedView {

	private Compiler compiler;

	private String charset;

	/**
	 * Set the JMustache compiler to be used by this view. Typically this property is not
	 * set directly. Instead a single {@link Compiler} is expected in the Spring
	 * application context which is used to compile Mustache templates.
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
	public boolean checkResourceExists(Locale locale) throws Exception {
		return resolveResource() != null;
	}

	@Override
	protected Mono<Void> renderInternal(Map<String, Object> model, MediaType contentType, ServerWebExchange exchange) {
		Resource resource = resolveResource();
		if (resource == null) {
			return Mono
					.error(new IllegalStateException("Could not find Mustache template with URL [" + getUrl() + "]"));
		}
		DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
		try (Reader reader = getReader(resource)) {
			Template template = this.compiler.compile(reader);
			Charset charset = getCharset(contentType).orElseGet(this::getDefaultCharset);
			try (Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset)) {
				template.execute(model, writer);
				writer.flush();
			}
		}
		catch (Exception ex) {
			DataBufferUtils.release(dataBuffer);
			return Mono.error(ex);
		}
		return exchange.getResponse().writeWith(Flux.just(dataBuffer));
	}

	private Resource resolveResource() {
		Resource resource = getApplicationContext().getResource(getUrl());
		if (resource == null || !resource.exists()) {
			return null;
		}
		return resource;
	}

	private Reader getReader(Resource resource) throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(resource.getInputStream(), this.charset);
		}
		return new InputStreamReader(resource.getInputStream());
	}

	private Optional<Charset> getCharset(MediaType mediaType) {
		return Optional.ofNullable((mediaType != null) ? mediaType.getCharset() : null);
	}

}
