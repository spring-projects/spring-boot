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

package org.springframework.boot.autoconfigure;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.validation.Configuration;
import jakarta.validation.Validation;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

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

	/**
	 * Returns the order of the method in the execution sequence.
	 * @return the order of the method
	 */
	@Override
	public int getOrder() {
		return LoggingApplicationListener.DEFAULT_ORDER + 1;
	}

	/**
	 * This method is called when an application event is triggered. It performs
	 * preinitialization if the application is enabled and not running in a native image.
	 * If the event is an ApplicationEnvironmentPreparedEvent and preinitialization has
	 * not started yet, it calls the performPreinitialization() method. If the event is an
	 * ApplicationReadyEvent or ApplicationFailedEvent and preinitialization has started,
	 * it waits for the preinitialization to complete.
	 * @param event the application event that triggered this method
	 */
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

	/**
	 * Performs preinitialization tasks in a separate background thread.
	 *
	 * This method creates a new thread and runs a series of preinitialization tasks in
	 * that thread. The tasks include initializing conversion service, validation, message
	 * converter, Jackson, charset, Tomcat, and JDK. If any of the tasks fail, the method
	 * continues to run the remaining tasks. The completion of preinitialization is
	 * signaled by a countdown latch.
	 *
	 * If creating a new thread fails, such as in the case of Google App Engine where
	 * thread creation is prohibited, the preinitialization tasks are run on the main
	 * thread, resulting in slightly slower startup.
	 * @throws Exception if an exception occurs while creating the background thread
	 */
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
					runSafely(new TomcatInitializer());
					runSafely(new JdkInitializer());
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
	private static final class MessageConverterInitializer implements Runnable {

		/**
		 * This method is the entry point for running the MessageConverterInitializer
		 * class. It creates a new instance of the AllEncompassingFormHttpMessageConverter
		 * class.
		 */
		@Override
		public void run() {
			new AllEncompassingFormHttpMessageConverter();
		}

	}

	/**
	 * Early initializer for jakarta.validation.
	 */
	private static final class ValidationInitializer implements Runnable {

		/**
		 * This method is used to run the validation process. It configures the default
		 * provider and builds the validator factory.
		 */
		@Override
		public void run() {
			Configuration<?> configuration = Validation.byDefaultProvider().configure();
			configuration.buildValidatorFactory().getValidator();
		}

	}

	/**
	 * Early initializer for Jackson.
	 */
	private static final class JacksonInitializer implements Runnable {

		/**
		 * This method is used to initialize the Jackson ObjectMapper with JSON
		 * configuration. It uses the Jackson2ObjectMapperBuilder to build the
		 * ObjectMapper with JSON format.
		 * @return void
		 */
		@Override
		public void run() {
			Jackson2ObjectMapperBuilder.json().build();
		}

	}

	/**
	 * Early initializer for Spring's ConversionService.
	 */
	private static final class ConversionServiceInitializer implements Runnable {

		/**
		 * This method is the entry point for the program and is responsible for
		 * initializing the ConversionService. It creates a new instance of
		 * DefaultFormattingConversionService and sets it as the default
		 * ConversionService.
		 */
		@Override
		public void run() {
			new DefaultFormattingConversionService();
		}

	}

	/**
	 * CharsetInitializer class.
	 */
	private static final class CharsetInitializer implements Runnable {

		/**
		 * This method is the implementation of the run() method from the Runnable
		 * interface. It retrieves the name of the UTF-8 character set using the
		 * StandardCharsets class.
		 */
		@Override
		public void run() {
			StandardCharsets.UTF_8.name();
		}

	}

	/**
	 * TomcatInitializer class.
	 */
	private static final class TomcatInitializer implements Runnable {

		/**
		 * This method is the entry point for the TomcatInitializer class. It is
		 * responsible for initializing the necessary components for the Tomcat server.
		 *
		 * The method creates a new instance of the Rfc6265CookieProcessor class and the
		 * NonLoginAuthenticator class. These components are essential for handling
		 * cookies and authentication in the Tomcat server.
		 *
		 * @see Rfc6265CookieProcessor
		 * @see NonLoginAuthenticator
		 */
		@Override
		public void run() {
			new Rfc6265CookieProcessor();
			new NonLoginAuthenticator();
		}

	}

	/**
	 * JdkInitializer class.
	 */
	private static final class JdkInitializer implements Runnable {

		/**
		 * This method is used to run the JdkInitializer class. It sets the system default
		 * time zone to the current system's default time zone.
		 */
		@Override
		public void run() {
			ZoneId.systemDefault();
		}

	}

}
