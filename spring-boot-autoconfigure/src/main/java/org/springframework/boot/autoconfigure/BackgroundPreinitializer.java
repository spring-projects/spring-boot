/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.validation.Validation;

import org.apache.catalina.mbeans.MBeanFactory;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;

/**
 * {@link ApplicationListener} to trigger early initialization in a background thread of
 * time consuming tasks.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class BackgroundPreinitializer
		implements ApplicationListener<ApplicationStartedEvent> {

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		submit(executor, new MessageConverterInitializer());
		submit(executor, new MBeanFactoryInitializer());
		submit(executor, new ValidationInitializer());
		executor.shutdown();
	}

	private void submit(ExecutorService executor, Runnable runnable) {
		executor.submit(new FailSafeRunnable(runnable));
	}

	/**
	 * Wrapper to ignore any thrown exceptions.
	 */
	private static class FailSafeRunnable implements Runnable {

		private final Runnable delegate;

		FailSafeRunnable(Runnable delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run() {
			try {
				this.delegate.run();
			}
			catch (Throwable ex) {
				// Ignore
			}
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
	 * Early initializer to load Tomcat MBean XML.
	 */
	private static class MBeanFactoryInitializer implements Runnable {

		@Override
		public void run() {
			new MBeanFactory();
		}

	}

	/**
	 * Early initializer for javax.validation.
	 */
	private static class ValidationInitializer implements Runnable {

		@Override
		public void run() {
			Validation.byDefaultProvider().configure();
		}

	}
}
