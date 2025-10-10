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

package org.springframework.boot.tomcat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.session.ManagerBase;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Tomcat {@link StandardContext} used by {@link TomcatWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class TomcatEmbeddedContext extends StandardContext {

	private DeferredStartupExceptions deferredStartupExceptions = DeferredStartupExceptions.NONE;

	private @Nullable MimeMappings mimeMappings;

	@Override
	public boolean loadOnStartup(Container[] children) {
		// deferred until later (see deferredLoadOnStartup)
		return true;
	}

	@Override
	public void setManager(Manager manager) {
		if (manager instanceof ManagerBase) {
			manager.setSessionIdGenerator(new LazySessionIdGenerator());
		}
		super.setManager(manager);
	}

	void deferredLoadOnStartup() {
		doWithThreadContextClassLoader(getLoader().getClassLoader(),
				() -> getLoadOnStartupWrappers(findChildren()).forEach(this::load));
	}

	private Stream<Wrapper> getLoadOnStartupWrappers(Container[] children) {
		Map<Integer, List<Wrapper>> grouped = new TreeMap<>();
		for (Container child : children) {
			Wrapper wrapper = (Wrapper) child;
			int order = wrapper.getLoadOnStartup();
			if (order >= 0) {
				grouped.computeIfAbsent(order, (o) -> new ArrayList<>()).add(wrapper);
			}
		}
		return grouped.values().stream().flatMap(List::stream);
	}

	private void load(Wrapper wrapper) {
		try {
			wrapper.load();
		}
		catch (ServletException ex) {
			String message = sm.getString("standardContext.loadOnStartup.loadException", getName(), wrapper.getName());
			if (getComputedFailCtxIfServletStartFails()) {
				throw new WebServerException(message, ex);
			}
			getLogger().error(message, StandardWrapper.getRootCause(ex));
		}
	}

	/**
	 * Some older Servlet frameworks (e.g. Struts, BIRT) use the Thread context class
	 * loader to create servlet instances in this phase. If they do that and then try to
	 * initialize them later the class loader may have changed, so wrap the call to
	 * loadOnStartup in what we think is going to be the main webapp classloader at
	 * runtime.
	 * @param classLoader the class loader to use
	 * @param code the code to run
	 */
	private void doWithThreadContextClassLoader(@Nullable ClassLoader classLoader, Runnable code) {
		ClassLoader existingLoader = (classLoader != null) ? ClassUtils.overrideThreadContextClassLoader(classLoader)
				: null;
		try {
			code.run();
		}
		finally {
			if (existingLoader != null) {
				ClassUtils.overrideThreadContextClassLoader(existingLoader);
			}
		}
	}

	/**
	 * Set the a strategy used to capture and rethrow deferred startup exceptions.
	 * @param deferredStartupExceptions the strategy to use
	 */
	public void setDeferredStartupExceptions(DeferredStartupExceptions deferredStartupExceptions) {
		Assert.notNull(deferredStartupExceptions, "'deferredStartupExceptions' must not be null");
		this.deferredStartupExceptions = deferredStartupExceptions;
	}

	DeferredStartupExceptions getDeferredStartupExceptions() {
		return this.deferredStartupExceptions;
	}

	public void setMimeMappings(MimeMappings mimeMappings) {
		this.mimeMappings = mimeMappings;
	}

	@Override
	public String[] findMimeMappings() {
		List<String> mappings = new ArrayList<>(Arrays.asList(super.findMimeMappings()));
		if (this.mimeMappings != null) {
			this.mimeMappings.forEach((mapping) -> mappings.add(mapping.getExtension()));
		}
		return mappings.toArray(String[]::new);
	}

	@Override
	public @Nullable String findMimeMapping(String extension) {
		String mimeMapping = super.findMimeMapping(extension);
		if (mimeMapping != null) {
			return mimeMapping;
		}
		return (this.mimeMappings != null) ? this.mimeMappings.get(extension) : null;
	}

	/**
	 * Strategy interface that can be used to rethrow deferred startup exceptions.
	 */
	@FunctionalInterface
	public interface DeferredStartupExceptions {

		/**
		 * {@link DeferredStartupExceptions} that does nothing.
		 */
		DeferredStartupExceptions NONE = () -> {
		};

		/**
		 * Rethrow deferred startup exceptions if there are any.
		 * @throws Exception the deferred startup exception
		 */
		void rethrow() throws Exception;

	}

}
