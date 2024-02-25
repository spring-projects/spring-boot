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

package org.springframework.boot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.util.Assert;

/**
 * A {@link Runnable} to be used as a {@link Runtime#addShutdownHook(Thread) shutdown
 * hook} to perform graceful shutdown of Spring Boot applications. This hook tracks
 * registered application contexts as well as any actions registered via
 * {@link SpringApplication#getShutdownHandlers()}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Brian Clozel
 */
class SpringApplicationShutdownHook implements Runnable {

	private static final int SLEEP = 50;

	private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

	private static final Log logger = LogFactory.getLog(SpringApplicationShutdownHook.class);

	private final Handlers handlers = new Handlers();

	private final Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();

	private final Set<ConfigurableApplicationContext> closedContexts = Collections.newSetFromMap(new WeakHashMap<>());

	private final ApplicationContextClosedListener contextCloseListener = new ApplicationContextClosedListener();

	private final AtomicBoolean shutdownHookAdded = new AtomicBoolean();

	private volatile boolean shutdownHookAdditionEnabled = false;

	private boolean inProgress;

	/**
     * Returns the handlers registered in the SpringApplicationShutdownHook.
     *
     * @return the handlers registered in the SpringApplicationShutdownHook
     */
    SpringApplicationShutdownHandlers getHandlers() {
		return this.handlers;
	}

	/**
     * Enables the addition of a shutdown hook.
     * 
     * This method sets the shutdownHookAdditionEnabled flag to true, allowing the addition of a shutdown hook.
     * A shutdown hook is a thread that gets executed when the JVM is shutting down, allowing for cleanup tasks
     * or other actions to be performed before the application exits.
     */
    void enableShutdownHookAddition() {
		this.shutdownHookAdditionEnabled = true;
	}

	/**
     * Registers the given application context with the shutdown hook.
     * 
     * @param context the application context to be registered
     * @throws IllegalStateException if the shutdown hook is already in progress
     */
    void registerApplicationContext(ConfigurableApplicationContext context) {
		addRuntimeShutdownHookIfNecessary();
		synchronized (SpringApplicationShutdownHook.class) {
			assertNotInProgress();
			context.addApplicationListener(this.contextCloseListener);
			this.contexts.add(context);
		}
	}

	/**
     * Adds a runtime shutdown hook if necessary.
     * 
     * This method checks if the shutdown hook addition is enabled and if the shutdown hook has not already been added.
     * If both conditions are met, it adds a runtime shutdown hook by calling the addRuntimeShutdownHook() method.
     * 
     * @see SpringApplicationShutdownHook#addRuntimeShutdownHook()
     */
    private void addRuntimeShutdownHookIfNecessary() {
		if (this.shutdownHookAdditionEnabled && this.shutdownHookAdded.compareAndSet(false, true)) {
			addRuntimeShutdownHook();
		}
	}

	/**
     * Adds a runtime shutdown hook to the current application.
     * This hook is responsible for executing the necessary cleanup tasks
     * when the application is shutting down.
     * 
     * @throws SecurityException if a security manager exists and its checkExit method doesn't allow the exit.
     * @throws IllegalArgumentException if the specified hook has already been registered, or if it can't be registered.
     * 
     * @see Runtime#addShutdownHook(Thread)
     * @see Thread
     * @see SpringApplicationShutdownHook
     */
    void addRuntimeShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(this, "SpringApplicationShutdownHook"));
	}

	/**
     * Deregisters a failed application context.
     * 
     * @param applicationContext the application context to be deregistered
     * @throws IllegalStateException if the application context is active
     */
    void deregisterFailedApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (SpringApplicationShutdownHook.class) {
			Assert.state(!applicationContext.isActive(), "Cannot unregister active application context");
			SpringApplicationShutdownHook.this.contexts.remove(applicationContext);
		}
	}

	/**
     * This method is responsible for running the shutdown process of the application.
     * It closes all the configurable application contexts, waits for them to close, and then executes any additional actions.
     * 
     * @Override
     * @see java.lang.Runnable#run()
     */
    @Override
	public void run() {
		Set<ConfigurableApplicationContext> contexts;
		Set<ConfigurableApplicationContext> closedContexts;
		Set<Runnable> actions;
		synchronized (SpringApplicationShutdownHook.class) {
			this.inProgress = true;
			contexts = new LinkedHashSet<>(this.contexts);
			closedContexts = new LinkedHashSet<>(this.closedContexts);
			actions = new LinkedHashSet<>(this.handlers.getActions());
		}
		contexts.forEach(this::closeAndWait);
		closedContexts.forEach(this::closeAndWait);
		actions.forEach(Runnable::run);
	}

	/**
     * Checks if the given ConfigurableApplicationContext is registered in the SpringApplicationShutdownHook.
     *
     * @param context the ConfigurableApplicationContext to be checked
     * @return true if the context is registered, false otherwise
     */
    boolean isApplicationContextRegistered(ConfigurableApplicationContext context) {
		synchronized (SpringApplicationShutdownHook.class) {
			return this.contexts.contains(context);
		}
	}

	/**
     * Resets the state of the SpringApplicationShutdownHook.
     * This method clears the contexts, closedContexts, and handlers actions.
     * It also sets the inProgress flag to false.
     */
    void reset() {
		synchronized (SpringApplicationShutdownHook.class) {
			this.contexts.clear();
			this.closedContexts.clear();
			this.handlers.getActions().clear();
			this.inProgress = false;
		}
	}

	/**
	 * Call {@link ConfigurableApplicationContext#close()} and wait until the context
	 * becomes inactive. We can't assume that just because the close method returns that
	 * the context is actually inactive. It could be that another thread is still in the
	 * process of disposing beans.
	 * @param context the context to clean
	 */
	private void closeAndWait(ConfigurableApplicationContext context) {
		if (!context.isActive()) {
			return;
		}
		context.close();
		try {
			int waited = 0;
			while (context.isActive()) {
				if (waited > TIMEOUT) {
					throw new TimeoutException();
				}
				Thread.sleep(SLEEP);
				waited += SLEEP;
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("Interrupted waiting for application context " + context + " to become inactive");
		}
		catch (TimeoutException ex) {
			logger.warn("Timed out waiting for application context " + context + " to become inactive", ex);
		}
	}

	/**
     * Asserts that the shutdown is not in progress.
     * 
     * @throws IllegalStateException if shutdown is in progress
     */
    private void assertNotInProgress() {
		Assert.state(!SpringApplicationShutdownHook.this.inProgress, "Shutdown in progress");
	}

	/**
	 * The handler actions for this shutdown hook.
	 */
	private final class Handlers implements SpringApplicationShutdownHandlers, Runnable {

		private final Set<Runnable> actions = Collections.newSetFromMap(new IdentityHashMap<>());

		/**
         * Adds a new action to be executed during the shutdown of the application.
         * 
         * @param action the action to be executed
         * @throws IllegalArgumentException if the action is null
         */
        @Override
		public void add(Runnable action) {
			Assert.notNull(action, "Action must not be null");
			addRuntimeShutdownHookIfNecessary();
			synchronized (SpringApplicationShutdownHook.class) {
				assertNotInProgress();
				this.actions.add(action);
			}
		}

		/**
         * Removes the specified action from the list of actions to be executed.
         * 
         * @param action the action to be removed (must not be null)
         * @throws IllegalArgumentException if the action is null
         * @throws IllegalStateException if the application shutdown is already in progress
         */
        @Override
		public void remove(Runnable action) {
			Assert.notNull(action, "Action must not be null");
			synchronized (SpringApplicationShutdownHook.class) {
				assertNotInProgress();
				this.actions.remove(action);
			}
		}

		/**
         * Returns a set of Runnable actions.
         *
         * @return a set of Runnable actions
         */
        Set<Runnable> getActions() {
			return this.actions;
		}

		/**
         * This method is used to run the SpringApplicationShutdownHook.
         * It calls the run() method of the SpringApplicationShutdownHook class and then resets it.
         */
        @Override
		public void run() {
			SpringApplicationShutdownHook.this.run();
			SpringApplicationShutdownHook.this.reset();
		}

	}

	/**
	 * {@link ApplicationListener} to track closed contexts.
	 */
	private final class ApplicationContextClosedListener implements ApplicationListener<ContextClosedEvent> {

		/**
         * This method is called when the application context is closed.
         * It is triggered by the ContextClosedEvent.
         * 
         * The ContextClosedEvent is fired at the start of a call to close()
         * and if that happens in a different thread then the context may still be
         * active. To handle this, the context is added to a closedContexts set
         * and removed from the contexts set. The closedContexts set is a weak set
         * so that the context can be garbage collected once the close() method returns.
         * 
         * @param event The ContextClosedEvent that triggered this method
         */
        @Override
		public void onApplicationEvent(ContextClosedEvent event) {
			// The ContextClosedEvent is fired at the start of a call to {@code close()}
			// and if that happens in a different thread then the context may still be
			// active. Rather than just removing the context, we add it to a {@code
			// closedContexts} set. This is weak set so that the context can be GC'd once
			// the {@code close()} method returns.
			synchronized (SpringApplicationShutdownHook.class) {
				ApplicationContext applicationContext = event.getApplicationContext();
				SpringApplicationShutdownHook.this.contexts.remove(applicationContext);
				SpringApplicationShutdownHook.this.closedContexts
					.add((ConfigurableApplicationContext) applicationContext);
			}
		}

	}

}
