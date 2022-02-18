/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.lang.Thread.State;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SpringApplicationShutdownHook}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 */
class SpringApplicationShutdownHookTests {

	@Test
	void shutdownHookIsNotAddedUntilContextIsRegistered() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		assertThat(shutdownHook.isRuntimeShutdownHookAdded()).isFalse();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		shutdownHook.registerApplicationContext(context);
		assertThat(shutdownHook.isRuntimeShutdownHookAdded()).isTrue();
	}

	@Test
	void runClosesContextsBeforeRunningHandlerActions() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		List<Object> finished = new CopyOnWriteArrayList<>();
		ConfigurableApplicationContext context = new TestApplicationContext(finished);
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		Runnable handlerAction = new TestHandlerAction(finished);
		shutdownHook.getHandlers().add(handlerAction);
		shutdownHook.run();
		assertThat(finished).containsExactly(context, handlerAction);
	}

	@Test
	void runWhenContextIsBeingClosedInAnotherThreadWaitsUntilContextIsInactive() throws InterruptedException {
		// This situation occurs in the Spring Tools IDE. It triggers a context close via
		// JMX and then stops the JVM. The two actions happen almost simultaneously
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		List<Object> finished = new CopyOnWriteArrayList<>();
		CountDownLatch closing = new CountDownLatch(1);
		CountDownLatch proceedWithClose = new CountDownLatch(1);
		ConfigurableApplicationContext context = new TestApplicationContext(finished, closing, proceedWithClose);
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		Runnable handlerAction = new TestHandlerAction(finished);
		shutdownHook.getHandlers().add(handlerAction);
		Thread contextThread = new Thread(context::close);
		contextThread.start();
		// Wait for context thread to begin closing the context
		closing.await();
		Thread shutdownThread = new Thread(shutdownHook);
		shutdownThread.start();
		// Shutdown thread should become blocked on monitor held by context thread
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(shutdownThread::getState, State.BLOCKED::equals);
		// Allow context thread to proceed, unblocking shutdown thread
		proceedWithClose.countDown();
		contextThread.join();
		shutdownThread.join();
		// Context should have been closed before handler action was run
		assertThat(finished).containsExactly(context, handlerAction);
	}

	@Test
	void runDueToExitDuringRefreshWhenContextHasBeenClosedDoesNotDeadlock() throws InterruptedException {
		GenericApplicationContext context = new GenericApplicationContext();
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		shutdownHook.registerApplicationContext(context);
		context.registerBean(CloseContextAndExit.class, context, shutdownHook);
		context.refresh();
	}

	@Test
	void runWhenContextIsClosedDirectlyRunsHandlerActions() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		List<Object> finished = new CopyOnWriteArrayList<>();
		ConfigurableApplicationContext context = new TestApplicationContext(finished);
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		context.close();
		Runnable handlerAction1 = new TestHandlerAction(finished);
		Runnable handlerAction2 = new TestHandlerAction(finished);
		shutdownHook.getHandlers().add(handlerAction1);
		shutdownHook.getHandlers().add(handlerAction2);
		shutdownHook.run();
		assertThat(finished).contains(handlerAction1, handlerAction2);
	}

	@Test
	void addHandlerActionWhenNullThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		assertThatIllegalArgumentException().isThrownBy(() -> shutdownHook.getHandlers().add(null))
				.withMessage("Action must not be null");
	}

	@Test
	void addHandlerActionWhenShuttingDownThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		shutdownHook.run();
		Runnable handlerAction = new TestHandlerAction(new ArrayList<>());
		assertThatIllegalStateException().isThrownBy(() -> shutdownHook.getHandlers().add(handlerAction))
				.withMessage("Shutdown in progress");
	}

	@Test
	void removeHandlerActionWhenNullThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		assertThatIllegalArgumentException().isThrownBy(() -> shutdownHook.getHandlers().remove(null))
				.withMessage("Action must not be null");
	}

	@Test
	void removeHandlerActionWhenShuttingDownThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		Runnable handlerAction = new TestHandlerAction(new ArrayList<>());
		shutdownHook.getHandlers().add(handlerAction);
		shutdownHook.run();
		assertThatIllegalStateException().isThrownBy(() -> shutdownHook.getHandlers().remove(handlerAction))
				.withMessage("Shutdown in progress");
	}

	@Test
	void failsWhenDeregisterActiveContext() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		assertThatThrownBy(() -> shutdownHook.deregisterFailedApplicationContext(context))
				.isInstanceOf(IllegalStateException.class);
		assertThat(shutdownHook.isApplicationContextRegistered(context)).isTrue();
	}

	@Test
	void deregistersFailedContext() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		GenericApplicationContext context = new GenericApplicationContext();
		shutdownHook.registerApplicationContext(context);
		context.registerBean(FailingBean.class);
		assertThatThrownBy(context::refresh).isInstanceOf(BeanCreationException.class);
		assertThat(shutdownHook.isApplicationContextRegistered(context)).isTrue();
		shutdownHook.deregisterFailedApplicationContext(context);
		assertThat(shutdownHook.isApplicationContextRegistered(context)).isFalse();
	}

	static class TestSpringApplicationShutdownHook extends SpringApplicationShutdownHook {

		private boolean runtimeShutdownHookAdded;

		@Override
		protected void addRuntimeShutdownHook() {
			this.runtimeShutdownHookAdded = true;
		}

		boolean isRuntimeShutdownHookAdded() {
			return this.runtimeShutdownHookAdded;
		}

	}

	static class TestApplicationContext extends AbstractApplicationContext {

		private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		private final List<Object> finished;

		private final CountDownLatch closing;

		private final CountDownLatch proceedWithClose;

		TestApplicationContext(List<Object> finished) {
			this(finished, null, null);
		}

		TestApplicationContext(List<Object> finished, CountDownLatch closing, CountDownLatch proceedWithClose) {
			this.finished = finished;
			this.closing = closing;
			this.proceedWithClose = proceedWithClose;
		}

		@Override
		protected void refreshBeanFactory() {
		}

		@Override
		protected void closeBeanFactory() {
		}

		@Override
		protected void onClose() {
			if (this.closing != null) {
				this.closing.countDown();
			}
			if (this.proceedWithClose != null) {
				try {
					this.proceedWithClose.await();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			this.finished.add(this);
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return this.beanFactory;
		}

	}

	static class TestHandlerAction implements Runnable {

		private final List<Object> finished;

		TestHandlerAction(List<Object> finished) {
			this.finished = finished;
		}

		@Override
		public void run() {
			this.finished.add(this);
		}

	}

	static class CloseContextAndExit implements InitializingBean {

		private final ConfigurableApplicationContext context;

		private final Runnable shutdownHook;

		CloseContextAndExit(ConfigurableApplicationContext context, SpringApplicationShutdownHook shutdownHook) {
			this.context = context;
			this.shutdownHook = shutdownHook;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			this.context.close();
			// Simulate System.exit by running the hook on a separate thread and waiting
			// for it to complete
			Thread thread = new Thread(this.shutdownHook);
			thread.start();
			thread.join(15000);
			assertThat(thread.isAlive()).isFalse();
		}

	}

	static class FailingBean implements InitializingBean {

		@Override
		public void afterPropertiesSet() throws Exception {
			throw new IllegalArgumentException("test failure");
		}

	}

}
