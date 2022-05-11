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

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Low-level hooks that can observe a {@link SpringApplication} and modify its behavior.
 * Hooks are managed on a per-thread basis providing isolation when multiple applications
 * are executed in parallel.
 *
 * @author Andy Wilkinson
 */
final class SpringApplicationHooks {

	private static final ThreadLocal<Hooks> hooks = ThreadLocal.withInitial(Hooks::new);

	private SpringApplicationHooks() {
	}

	/**
	 * Runs the given {@code action} with the given {@code hook} attached.
	 * @param hook the hook to attach
	 * @param action the action to run
	 * @param <T> the type of the action's result
	 * @return the result of the action
	 * @throws Exception if a failure occurs while performing the action
	 */
	static <T> T withHook(Hook hook, ThrowingSupplier<T> action) throws Exception {
		hooks.get().add(hook);
		try {
			return action.getWithException();
		}
		finally {
			hooks.get().remove(hook);
		}
	}

	/**
	 * Runs the given {@code action} with the given {@code hook} attached.
	 * @param hook the hook to attach
	 * @param action the action to run
	 */
	static void withHook(Hook hook, Runnable action) {
		hooks.get().add(hook);
		try {
			action.run();
		}
		finally {
			hooks.get().remove(hook);
		}
	}

	static Hooks hooks() {
		return hooks.get();
	}

	/**
	 * A hook that can observe and modify the behavior of a {@link SpringApplication}.
	 */
	interface Hook {

		/**
		 * Called at the beginning of {@link SpringApplication#run(String...)}. Provides
		 * an opportunity to inspect and customise the application.
		 * @param application the application that is being run
		 */
		default void preRun(SpringApplication application) {
		}

		/**
		 * Called at the end of {@link SpringApplication#run(String...)}. Provides access
		 * to the {@link ConfigurableApplicationContext context} that has been created for
		 * the application.
		 * @param application the application that has been run
		 * @param context the application's context
		 */
		default void postRun(SpringApplication application, ConfigurableApplicationContext context) {
		}

		/**
		 * Called immediately before the given {@code context} is refreshed.
		 * @param application the application for which the context is being refreshed
		 * @param context the application's context
		 * @return whether to continue with refresh processing
		 */
		default boolean preRefresh(SpringApplication application, ConfigurableApplicationContext context) {
			return true;
		}

	}

	static final class Hooks implements Hook {

		private final List<Hook> delegates = new ArrayList<>();

		private void add(Hook hook) {
			this.delegates.add(hook);
		}

		private void remove(Hook hook) {
			this.delegates.remove(hook);
		}

		@Override
		public void preRun(SpringApplication application) {
			for (Hook delegate : this.delegates) {
				delegate.preRun(application);
			}
		}

		@Override
		public void postRun(SpringApplication application, ConfigurableApplicationContext context) {
			for (Hook delegate : this.delegates) {
				delegate.postRun(application, context);
			}
		}

		@Override
		public boolean preRefresh(SpringApplication application, ConfigurableApplicationContext context) {
			for (Hook delegate : this.delegates) {
				if (!delegate.preRefresh(application, context)) {
					return false;
				}
			}
			return true;
		}

	}

}
