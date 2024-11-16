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
 * on the {@link ApplicationStartingEvent} then updated with {@link EventPublisher} beans
 * as needed.
 * <p>
 * The {@link #addWrapper()} method may also be called directly if the
 * {@link ApplicationStartingEvent} isn't called early enough or isn't fired.
 *
 * @author Phillip Webb
 * @since 3.4.0
 * @see OpenTelemetryEventPublisherBeansTestExecutionListener
 */
public class OpenTelemetryEventPublisherBeansApplicationListener implements GenericApplicationListener {

	private static final boolean OTEL_CONTEXT_PRESENT = ClassUtils.isPresent("io.opentelemetry.context.ContextStorage",
			null);

	private static final boolean MICROMETER_OTEL_PRESENT = ClassUtils
		.isPresent("io.micrometer.tracing.otel.bridge.OtelTracer", null);

	private static final AtomicBoolean added = new AtomicBoolean();

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
		if (!isInstallable()) {
			return;
		}
		if (event instanceof ApplicationStartingEvent) {
			addWrapper();
		}
		if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
			ApplicationContext applicationContext = contextRefreshedEvent.getApplicationContext();
			List<EventPublishingContextWrapper> publishers = applicationContext
				.getBeansOfType(EventPublisher.class, true, false)
				.values()
				.stream()
				.map(EventPublishingContextWrapper::new)
				.toList();
			Wrapper.instance.put(applicationContext, publishers);
		}
		if (event instanceof ContextClosedEvent contextClosedEvent) {
			Wrapper.instance.remove(contextClosedEvent.getApplicationContext());
		}
	}

	/**
	 * {@link ContextStorage#addWrapper(java.util.function.Function) Add} the
	 * {@link ContextStorage} wrapper to ensure that {@link EventPublisher
	 * EventPublishers} are propagated correctly.
	 */
	public static void addWrapper() {
		if (isInstallable() && added.compareAndSet(false, true)) {
			Wrapper.instance.addWrapper();
		}
	}

	private static boolean isInstallable() {
		return OTEL_CONTEXT_PRESENT && MICROMETER_OTEL_PRESENT;
	}

	/**
	 * Single instance class used to add the wrapper and manage the {@link EventPublisher}
	 * beans.
	 */
	static final class Wrapper {

		static final Wrapper instance = new Wrapper();

		private final MultiValueMap<ApplicationContext, EventPublishingContextWrapper> beans = new LinkedMultiValueMap<>();

		private volatile ContextStorage storageDelegate;

		private Wrapper() {
		}

		private void addWrapper() {
			ContextStorage.addWrapper(Storage::new);
		}

		void put(ApplicationContext applicationContext, List<EventPublishingContextWrapper> publishers) {
			synchronized (this) {
				this.beans.addAll(applicationContext, publishers);
				this.storageDelegate = null;
			}
		}

		void remove(ApplicationContext applicationContext) {
			synchronized (this) {
				this.beans.remove(applicationContext);
				this.storageDelegate = null;
			}
		}

		ContextStorage getStorageDelegate(ContextStorage parent) {
			ContextStorage delegate = this.storageDelegate;
			if (delegate == null) {
				synchronized (this) {
					delegate = this.storageDelegate;
					if (delegate == null) {
						delegate = parent;
						for (List<EventPublishingContextWrapper> publishers : this.beans.values()) {
							for (EventPublishingContextWrapper publisher : publishers) {
								delegate = publisher.apply(delegate);
							}
						}
						this.storageDelegate = delegate;
					}
				}
			}
			return delegate;
		}

		/**
		 * {@link ContextStorage} that delegates to the {@link EventPublisher} beans.
		 */
		class Storage implements ContextStorage {

			private final ContextStorage parent;

			Storage(ContextStorage parent) {
				this.parent = parent;
			}

			@Override
			public Scope attach(Context toAttach) {
				return getDelegate().attach(toAttach);
			}

			@Override
			public Context current() {
				return getDelegate().current();
			}

			@Override
			public Context root() {
				return getDelegate().root();
			}

			private ContextStorage getDelegate() {
				return getStorageDelegate(this.parent);
			}

		}

	}

}
