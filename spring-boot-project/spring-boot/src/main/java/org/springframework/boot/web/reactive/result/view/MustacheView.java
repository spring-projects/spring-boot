/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;
import com.samskivert.mustache.Template.Fragment;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.server.ServerWebExchange;

/**
 * Spring WebFlux {@link View} using the Mustache template engine.
 *
 * @author Brian Clozel, Dave Syer
 * @since 2.0.0
 */
public class MustacheView extends AbstractUrlBasedView {

	private Compiler compiler;

	private String charset;

	private static Map<Resource, Template> templates = new HashMap<>();

	private boolean cache = true;

	/**
	 * Flag to indiciate that templates ought to be cached.
	 * @param cache the flag value
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

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
	protected Mono<Void> renderInternal(Map<String, Object> model, MediaType contentType,
			ServerWebExchange exchange) {
		Resource resource = resolveResource();
		if (resource == null) {
			return Mono.error(new IllegalStateException(
					"Could not find Mustache template with URL [" + getUrl() + "]"));
		}
		boolean sse = MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType);
		Charset charset = getCharset(contentType).orElse(getDefaultCharset());
		ServerHttpResponse response = exchange.getResponse();
		FluxWriter writer = new FluxWriter(response.bufferFactory(), charset);
		Mono<Template> rendered;
		if (!this.cache || !templates.containsKey(resource)) {
			rendered = Mono.fromCallable(() -> compile(resource))
					.subscribeOn(Schedulers.elastic());
		}
		else {
			rendered = Mono.just(templates.get(resource));
		}
		Map<String, Object> map;
		if (sse) {
			map = new HashMap<>(model);
			map.put("sse:data", new SseLambda());
		}
		else {
			map = model;
		}
		return rendered.flatMap((template) -> {
			template.execute(map, writer);
			return response.writeAndFlushWith(writer.getBuffers());
		}).doOnTerminate(() -> close(writer));
	}

	private void close(FluxWriter writer) {
		try {
			writer.close();
		}
		catch (IOException ex) {
			writer.release();
		}
	}

	private Template compile(Resource resource) {
		try {
			try (Reader reader = getReader(resource)) {
				Template template = this.compiler.compile(reader);
				return template;
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot close reader");
		}
	}

	@Override
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model) {
		Map<String, Object> result = new HashMap<>();
		for (String key : model.keySet()) {
			if (!key.startsWith("async:")) {
				result.put(key, model.get(key));
			}
			else {
				model.put(key, new FluxLambda((Publisher<?>) model.get(key)));
			}
		}
		return super.resolveAsyncAttributes(result)
				.doOnSuccess((v) -> model.putAll(result));
	}

	private Resource resolveResource() {
		Resource resource = getApplicationContext().getResource(getUrl());
		if (resource == null || !resource.exists()) {
			return null;
		}
		return resource;
	}

	private Reader getReader(Resource resource) throws IOException {
		Reader result;
		if (this.charset != null) {
			result = new InputStreamReader(resource.getInputStream(), this.charset);
		}
		else {
			result = new InputStreamReader(resource.getInputStream());
		}
		return result;
	}

	private Optional<Charset> getCharset(MediaType mediaType) {
		return Optional.ofNullable((mediaType != null) ? mediaType.getCharset() : null);
	}

	private static class FluxLambda implements Mustache.Lambda {

		private Publisher<?> publisher;

		FluxLambda(Publisher<?> publisher) {
			this.publisher = publisher;
		}

		@Override
		public void execute(Fragment frag, Writer out) throws IOException {
			try {
				if (out instanceof FluxWriter) {
					FluxWriter fluxWriter = (FluxWriter) out;
					fluxWriter.flush();
					fluxWriter.write(Flux.from(this.publisher).map(frag::execute));
				}
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	private static class SseLambda implements Mustache.Lambda {

		@Override
		public void execute(Fragment frag, Writer out) throws IOException {
			try {
				StringWriter writer = new StringWriter();
				frag.execute(writer);
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(
						new ByteArrayInputStream(writer.toString().getBytes())))) {
					reader.lines().forEach((line) -> {
						try {
							out.write("data: " + line + "\n");
						}
						catch (IOException ex) {
							throw new IllegalStateException("Cannot write data", ex);
						}
					});
				}
				out.write(new char[] { '\n', '\n' });
			}
			catch (IOException ex) {
				throw new IllegalStateException("Cannot render SSE data", ex);
			}
		}

	}

}
