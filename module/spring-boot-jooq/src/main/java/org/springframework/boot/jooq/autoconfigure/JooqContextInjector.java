/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.reactive.ContextInjector;
import kotlinx.coroutines.reactor.ReactorContext;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.util.ClassUtils;

/**
 * Propagates a coroutine's Reactor context to jOOQ publishers.
 *
 * @author George Papadopoulos
 * @since 4.2.0
 */
public final class JooqContextInjector implements ContextInjector {

	private static final boolean reactorContextPresent = ClassUtils
		.isPresent("kotlinx.coroutines.reactor.ReactorContext", JooqContextInjector.class.getClassLoader());

	@Override
	public <T> Publisher<T> injectCoroutineContext(Publisher<T> publisher, CoroutineContext coroutineContext) {
		ReactorContext reactorContext = coroutineContext.get(ReactorContext.Key);
		if (reactorContext == null) {
			return publisher;
		}
		if (reactorContextPresent && publisher instanceof org.jooq.Publisher<?>) {
			// Bridge with jooq:
			// `Flux.from(publisher)` is jooq's documented way to turn its bridge
			// `Publisher` into something Reactor-native.
			// Though, wrapping alone only gets us a real Flux; the context still needs a
			// path into jooq's internals.
			// That path is the `SubscriberProvider` SPI, see `CoreSubscriberProvider`.
			return Flux.from(publisher).contextWrite(reactorContext.getContext());
		}
		return publisher;
	}

}
