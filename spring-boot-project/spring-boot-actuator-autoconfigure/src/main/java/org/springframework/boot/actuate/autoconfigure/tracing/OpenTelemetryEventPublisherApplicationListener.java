/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ApplicationListener} to add an OpenTelemetry {@link ContextStorage} wrapper for
 * {@link EventPublisher} bean support. A single {@link ContextStorage} wrapper is added
 * as early as possible then updated with {@link EventPublisher} beans as needed.
 *
 * @author Phillip Webb
 */
class OpenTelemetryEventPublisherApplicationListener implements GenericApplicationListener {

	private static final boolean OTEL_CONTEXT_PRESENT = ClassUtils.isPresent("io.opentelemetry.context.ContextStorage",
			null);

	private static final boolean MICROMETER_OTEL_PRESENT = ClassUtils
		.isPresent("io.micrometer.tracing.otel.bridge.OtelTracer", null);

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		Class<?> type = eventType.getRawClass();
		return (type != null) && (ApplicationStartingEvent.class.isAssignableFrom(type)
				|| ContextRefreshedEvent.class.isAssignableFrom(type)
				|| ContextClosedEvent.class.isAssignableFrom(type));
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (!OTEL_CONTEXT_PRESENT || !MICROMETER_OTEL_PRESENT) {
			return;
		}
		if (event instanceof ApplicationStartingEvent) {
			EventPublisherBeansContextWrapper.addWrapperIfNecessary();
		}
		if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
			ApplicationContext applicationContext = contextRefreshedEvent.getApplicationContext();
			List<EventPublishingContextWrapper> publishers = applicationContext
				.getBeansOfType(EventPublisher.class, true, false)
				.values()
				.stream()
				.map(EventPublishingContextWrapper::new)
				.toList();
			EventPublisherBeansContextWrapper.instance.put(applicationContext, publishers);
		}
		if (event instanceof ContextClosedEvent contextClosedEvent) {
			EventPublisherBeansContextWrapper.instance.remove(contextClosedEvent.getApplicationContext());
		}
	}

	/**
	 * The single {@link ContextStorage} wrapper that delegates to {@link EventPublisher}
	 * beans.
	 */
	static class EventPublisherBeansContextWrapper implements UnaryOperator<ContextStorage> {

		private static final AtomicBoolean added = new AtomicBoolean();

		private static final EventPublisherBeansContextWrapper instance = new EventPublisherBeansContextWrapper();

		private final MultiValueMap<ApplicationContext, EventPublishingContextWrapper> publishers = new LinkedMultiValueMap<>();

		private volatile ContextStorage delegate;

		static void addWrapperIfNecessary() {
			if (added.compareAndSet(false, true)) {
				ContextStorage.addWrapper(instance);
			}
		}

		@Override
		public ContextStorage apply(ContextStorage contextStorage) {
			return new EventPublisherBeansContextStorage(contextStorage);
		}

		void put(ApplicationContext applicationContext, List<EventPublishingContextWrapper> publishers) {
			synchronized (this) {
				this.publishers.addAll(applicationContext, publishers);
				this.delegate = null;
			}
		}

		void remove(ApplicationContext applicationContext) {
			synchronized (this) {
				this.publishers.remove(applicationContext);
				this.delegate = null;
			}
		}

		private ContextStorage getDelegate(ContextStorage parent) {
			ContextStorage delegate = this.delegate;
			if (delegate == null) {
				synchronized (this) {
					delegate = parent;
					for (List<EventPublishingContextWrapper> publishers : this.publishers.values()) {
						for (EventPublishingContextWrapper publisher : publishers) {
							delegate = publisher.apply(delegate);
						}
					}
				}
			}
			return delegate;
		}

		/**
		 * The wrapped {@link ContextStorage} that delegates to the
		 * {@link EventPublisherBeansContextWrapper}.
		 */
		class EventPublisherBeansContextStorage implements ContextStorage {

			private final ContextStorage parent;

			EventPublisherBeansContextStorage(ContextStorage wrapped) {
				this.parent = wrapped;
			}

			@Override
			public Scope attach(Context toAttach) {
				return getDelegate(this.parent).attach(toAttach);
			}

			@Override
			public Context current() {
				return getDelegate(this.parent).current();
			}

			@Override
			public Context root() {
				return getDelegate(this.parent).root();
			}

		}

	}

}
