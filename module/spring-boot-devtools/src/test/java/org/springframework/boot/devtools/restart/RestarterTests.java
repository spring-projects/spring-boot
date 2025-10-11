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

package org.springframework.boot.devtools.restart;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Restarter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class RestarterTests {

	@BeforeEach
	void setup() {
		RestarterInitializer.setRestarterInstance();
	}

	@AfterEach
	void cleanup() {
		Restarter.clearInstance();
	}

	@Test
	void cantGetInstanceBeforeInitialize() {
		Restarter.clearInstance();
		assertThatIllegalStateException().isThrownBy(Restarter::getInstance)
			.withMessageContaining("Restarter has not been initialized");
	}

	@Test
	void testRestart(CapturedOutput output) {
		Restarter.clearInstance();
		Thread thread = new Thread(SampleApplication::main);
		thread.start();
		Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
			assertThat(StringUtils.countOccurrencesOf(output.toString(), "Tick 0")).isGreaterThan(1);
			assertThat(StringUtils.countOccurrencesOf(output.toString(), "Tick 1")).isGreaterThan(1);
			assertThat(CloseCountingApplicationListener.closed).isGreaterThan(0);
		});
	}

	@Test
	void testDisabled() {
		Restarter.disable();
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		Restarter.getInstance().prepare(context);
		assertThat(Restarter.getInstance()).extracting("rootContexts", as(InstanceOfAssertFactories.LIST)).isEmpty();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void getOrAddAttributeWithNewAttribute() {
		ObjectFactory objectFactory = mock(ObjectFactory.class);
		given(objectFactory.getObject()).willReturn("abc");
		Object attribute = Restarter.getInstance().getOrAddAttribute("x", objectFactory);
		assertThat(attribute).isEqualTo("abc");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addUrlsMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> Restarter.getInstance().addUrls(null))
			.withMessageContaining("'urls' must not be null");
	}

	@Test
	void addUrls() throws Exception {
		URL url = new URL("file:/proj/module-a.jar!/");
		Collection<URL> urls = Collections.singleton(url);
		Restarter restarter = Restarter.getInstance();
		restarter.addUrls(urls);
		restarter.restart();
		ClassLoader classLoader = ((TestableRestarter) restarter).getRelaunchClassLoader();
		assertThat(classLoader).isNotNull();
		assertThat(((URLClassLoader) classLoader).getURLs()[0]).isEqualTo(url);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addClassLoaderFilesMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> Restarter.getInstance().addClassLoaderFiles(null))
			.withMessageContaining("'classLoaderFiles' must not be null");
	}

	@Test
	void addClassLoaderFiles() {
		ClassLoaderFiles classLoaderFiles = new ClassLoaderFiles();
		classLoaderFiles.addFile("f", new ClassLoaderFile(Kind.ADDED, "abc".getBytes()));
		Restarter restarter = Restarter.getInstance();
		restarter.addClassLoaderFiles(classLoaderFiles);
		restarter.restart();
		ClassLoader classLoader = ((TestableRestarter) restarter).getRelaunchClassLoader();
		assertThat(classLoader).isNotNull();
		assertThat(classLoader.getResourceAsStream("f")).hasContent("abc");
	}

	@Test
	void getOrAddAttributeWithExistingAttribute() {
		Restarter.getInstance().getOrAddAttribute("x", () -> "abc");
		ObjectFactory<?> objectFactory = mock(ObjectFactory.class);
		Object attribute = Restarter.getInstance().getOrAddAttribute("x", objectFactory);
		assertThat(attribute).isEqualTo("abc");
		then(objectFactory).shouldHaveNoInteractions();
	}

	@Test
	void getOrAddAttributeWithRecursion() {
		Restarter restarter = Restarter.getInstance();
		Object added = restarter.getOrAddAttribute("postgresContainer", () -> {
			restarter.getOrAddAttribute("rabbitContainer", () -> "def");
			return "abc";
		});
		ObjectFactory<?> objectFactory = mock(ObjectFactory.class);
		assertThat(added).isEqualTo("abc");
		assertThat(restarter.getOrAddAttribute("postgresContainer", objectFactory)).isEqualTo("abc");
		assertThat(restarter.getOrAddAttribute("rabbitContainer", objectFactory)).isEqualTo("def");
		then(objectFactory).shouldHaveNoInteractions();
	}

	@Test
	void getThreadFactory() throws Exception {
		final ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
		final ClassLoader contextClassLoader = new URLClassLoader(new URL[0]);
		Thread thread = new Thread(() -> {
			Runnable runnable = mock(Runnable.class);
			Thread regular = new Thread();
			ThreadFactory factory = Restarter.getInstance().getThreadFactory();
			Thread viaFactory = factory.newThread(runnable);
			// Regular threads will inherit the current thread
			assertThat(regular.getContextClassLoader()).isEqualTo(contextClassLoader);
			// Factory threads should inherit from the initial thread
			assertThat(viaFactory.getContextClassLoader()).isEqualTo(parentLoader);
		});
		thread.setContextClassLoader(contextClassLoader);
		thread.start();
		thread.join();
	}

	@Test
	void getInitialUrls() throws Exception {
		Restarter.clearInstance();
		RestartInitializer initializer = mock(RestartInitializer.class);
		URL[] urls = new URL[] { new URL("file:/proj/module-a.jar!/") };
		given(initializer.getInitialUrls(any(Thread.class))).willReturn(urls);
		Restarter.initialize(new String[0], false, initializer, false);
		assertThat(Restarter.getInstance().getInitialUrls()).isEqualTo(urls);
	}

	@Component
	@EnableScheduling
	static class SampleApplication {

		private int count = 0;

		private static final AtomicBoolean restart = new AtomicBoolean();

		@Scheduled(fixedDelay = 200)
		void tickBean() {
			System.out.println("Tick " + this.count++ + " " + Thread.currentThread());
		}

		@Scheduled(initialDelay = 500, fixedDelay = 500)
		void restart() {
			if (SampleApplication.restart.compareAndSet(false, true)) {
				Restarter.getInstance().restart();
			}
		}

		static void main(String... args) {
			Restarter.initialize(args, false, new MockRestartInitializer(), true);
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
					SampleApplication.class);
			context.addApplicationListener(new CloseCountingApplicationListener());
			Restarter.getInstance().prepare(context);
		}

	}

	static class CloseCountingApplicationListener implements ApplicationListener<ContextClosedEvent> {

		static int closed = 0;

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			closed++;
		}

	}

	static class TestableRestarter extends Restarter {

		private @Nullable ClassLoader relaunchClassLoader;

		TestableRestarter() {
			this(Thread.currentThread(), new String[] {}, false, new MockRestartInitializer());
		}

		protected TestableRestarter(Thread thread, String[] args, boolean forceReferenceCleanup,
				RestartInitializer initializer) {
			super(thread, args, forceReferenceCleanup, initializer);
		}

		@Override
		public void restart(FailureHandler failureHandler) {
			try {
				stop();
				start(failureHandler);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		protected @Nullable Throwable relaunch(ClassLoader classLoader) {
			this.relaunchClassLoader = classLoader;
			return null;
		}

		@Override
		protected void stop() {
		}

		@Nullable ClassLoader getRelaunchClassLoader() {
			return this.relaunchClassLoader;
		}

	}

	static class RestarterInitializer {

		static void setRestarterInstance() {
			main(new String[0]);
		}

		static void main(String[] args) {
			Restarter.setInstance(new TestableRestarter());
		}

	}

}
