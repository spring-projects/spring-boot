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

package org.springframework.boot.autoconfigure.preinitialize;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.NativeDetector;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * {@link ApplicationListener} to trigger early initialization in a background thread of
 * time-consuming tasks.
 * <p>
 * Set the {@link #IGNORE_BACKGROUNDPREINITIALIZER_PROPERTY_NAME} system property to
 * {@code true} to disable this mechanism.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @author Sebastien Deleuze
 * @see BackgroundPreinitializer
 */
class BackgroundPreinitializingApplicationListener implements ApplicationListener<SpringApplicationEvent>, Ordered {

	/**
	 * System property that instructs Spring Boot how to run pre initialization. When the
	 * property is set to {@code true}, no pre-initialization happens and each item is
	 * initialized in the foreground as it needs to. When the property is {@code false}
	 * (default), pre initialization runs in a separate thread in the background.
	 */
	public static final String IGNORE_BACKGROUNDPREINITIALIZER_PROPERTY_NAME = "spring.backgroundpreinitializer.ignore";

	private static final AtomicBoolean started = new AtomicBoolean();

	private static final CountDownLatch complete = new CountDownLatch(1);

	private final SpringFactoriesLoader factoriesLoader;

	private final boolean enabled;

	BackgroundPreinitializingApplicationListener() {
		this(SpringFactoriesLoader.forDefaultResourceLocation());
	}

	BackgroundPreinitializingApplicationListener(SpringFactoriesLoader factoriesLoader) {
		this.factoriesLoader = factoriesLoader;
		this.enabled = !NativeDetector.inNativeImage()
				&& !Boolean.getBoolean(IGNORE_BACKGROUNDPREINITIALIZER_PROPERTY_NAME)
				&& Runtime.getRuntime().availableProcessors() > 1;
	}

	@Override
	public int getOrder() {
		return LoggingApplicationListener.DEFAULT_ORDER + 1;
	}

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (!this.enabled) {
			return;
		}
		if (event instanceof ApplicationEnvironmentPreparedEvent && started.compareAndSet(false, true)) {
			preinitialize();
		}
		if ((event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent) && started.get()) {
			try {
				complete.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void preinitialize() {
		Runner runner = new Runner(this.factoriesLoader.load(BackgroundPreinitializer.class));
		try {
			Thread thread = new Thread(runner, "background-preinit");
			thread.start();
		}
		catch (Exception ex) {
			// This will fail on Google App Engine where creating threads is
			// prohibited. We can safely continue but startup will be slightly slower
			// as the initialization will now happen on the main thread.
			complete.countDown();
		}
	}

	/**
	 * Runner thread to call the {@link BackgroundPreinitializer} instances.
	 *
	 * @param preinitializers the preinitializers
	 */
	record Runner(List<BackgroundPreinitializer> preinitializers) implements Runnable {

		@Override
		public void run() {
			for (BackgroundPreinitializer preinitializer : this.preinitializers) {
				try {
					preinitializer.preinitialize();
				}
				catch (Throwable ex) {
				}
			}
			complete.countDown();
		}

	}

}
