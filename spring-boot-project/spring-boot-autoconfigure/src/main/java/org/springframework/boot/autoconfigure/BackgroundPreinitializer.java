/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.validation.Configuration;
import jakarta.validation.Validation;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.NativeDetector;
import org.springframework.core.Ordered;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;

/**
 * {@link ApplicationListener} to trigger early initialization in a background thread of
 * time-consuming tasks.
 * <p>
 * Set the {@link #IGNORE_BACKGROUNDPREINITIALIZER_PROPERTY_NAME} system property to
 * {@code true} to disable this mechanism and let such initialization happen in the
 * foreground.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @author Sebastien Deleuze
 * @since 1.3.0
 */
public class BackgroundPreinitializer implements ApplicationListener<SpringApplicationEvent>, Ordered {

	/**
	 * System property that instructs Spring Boot how to run pre initialization. When the
	 * property is set to {@code true}, no pre-initialization happens and each item is
	 * initialized in the foreground as it needs to. When the property is {@code false}
	 * (default), pre initialization runs in a separate thread in the background.
	 * @since 2.1.0
	 */
	public static final String IGNORE_BACKGROUNDPREINITIALIZER_PROPERTY_NAME = "spring.backgroundpreinitializer.ignore";

	private static final AtomicBoolean preinitializationStarted = new AtomicBoolean();

	private static final CountDownLatch preinitializationComplete = new CountDownLatch(1);

	private static final boolean ENABLED = !Boolean.getBoolean(IGNORE_BACKGROUNDPREINITIALIZER_PROPERTY_NAME)
			&& Runtime.getRuntime().availableProcessors() > 1;

	@Override
	public int getOrder() {
		return LoggingApplicationListener.DEFAULT_ORDER + 1;
	}

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (!ENABLED || NativeDetector.inNativeImage()) {
			return;
		}
		if (event instanceof ApplicationEnvironmentPreparedEvent
				&& preinitializationStarted.compareAndSet(false, true)) {
			performPreinitialization();
		}
		if ((event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent)
				&& preinitializationStarted.get()) {
			try {
				preinitializationComplete.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void performPreinitialization() {
		try {
			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					runSafely(new ConversionServiceInitializer());
					runSafely(new ValidationInitializer());
					if (!runSafely(new MessageConverterInitializer())) {
						// If the MessageConverterInitializer fails to run, we still might
						// be able to
						// initialize Jackson
						runSafely(new JacksonInitializer());
					}
					runSafely(new CharsetInitializer());
					preinitializationComplete.countDown();
				}

				boolean runSafely(Runnable runnable) {
					try {
						runnable.run();
						return true;
					}
					catch (Throwable ex) {
						return false;
					}
				}

			}, "background-preinit");
			thread.start();
		}
		catch (Exception ex) {
			// This will fail on GAE where creating threads is prohibited. We can safely
			// continue but startup will be slightly slower as the initialization will now
			// happen on the main thread.
			preinitializationComplete.countDown();
		}
	}

	/**
	 * Early initializer for Spring MessageConverters.
	 */
	private static class MessageConverterInitializer implements Runnable {

		@Override
		public void run() {
			new AllEncompassingFormHttpMessageConverter();
		}

	}

	/**
	 * Early initializer for jakarta.validation.
	 */
	private static class ValidationInitializer implements Runnable {

		@Override
		public void run() {
			Configuration<?> configuration = Validation.byDefaultProvider().configure();
			configuration.buildValidatorFactory().getValidator();
		}

	}

	/**
	 * Early initializer for Jackson.
	 */
	private static class JacksonInitializer implements Runnable {

		@Override
		public void run() {
			Jackson2ObjectMapperBuilder.json().build();
		}

	}

	/**
	 * Early initializer for Spring's ConversionService.
	 */
	private static class ConversionServiceInitializer implements Runnable {

		@Override
		public void run() {
			new DefaultFormattingConversionService();
		}

	}

	private static class CharsetInitializer implements Runnable {

		@Override
		public void run() {
			StandardCharsets.UTF_8.name();
		}

	}

}
