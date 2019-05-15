/*
 * Copyright 2019-2019 the original author or authors.
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
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * A {@link Writer} that can write a {@link Flux} (or {@link Publisher}) to a data buffer.
 * Used to render progressive output in a {@link MustacheView}.
 *
 * @author Dave Syer
 */
class FluxWriter extends Writer {

	private final DataBufferFactory factory;

	private final Charset charset;

	private List<Object> accumulated = new ArrayList<>();

	FluxWriter(DataBufferFactory factory, Charset charset) {
		this.factory = factory;
		this.charset = charset;
	}

	@SuppressWarnings("unchecked")
	public Flux<? extends Publisher<? extends DataBuffer>> getBuffers() {
		Flux<String> buffers = Flux.empty();
		List<String> chunks = new ArrayList<>();
		for (Object thing : this.accumulated) {
			if (thing instanceof Publisher) {
				buffers = concatValues(chunks, buffers);
				buffers = buffers.concatWith((Publisher<String>) thing);
			}
			else {
				chunks.add((String) thing);
			}
		}
		buffers = concatValues(chunks, buffers);
		return buffers.map((string) -> Mono.fromCallable(
				() -> this.factory.allocateBuffer().write(string, this.charset)));
	}

	private Flux<String> concatValues(List<String> chunks, Flux<String> buffers) {
		if (!chunks.isEmpty()) {
			buffers = buffers.concatWithValues(chunks.toArray(new String[0]));
			chunks.clear();
		}
		return buffers;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		this.accumulated.add(new String(cbuf, off, len));
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

	public void release() {
		// TODO: maybe implement this and call it on error
	}

	public void write(Object thing) {
		this.accumulated.add(thing);
	}

}
