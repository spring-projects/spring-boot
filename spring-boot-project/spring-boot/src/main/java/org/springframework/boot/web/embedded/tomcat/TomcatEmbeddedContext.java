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

package org.springframework.boot.web.embedded.tomcat;

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

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.ClassUtils;

/**
 * Tomcat {@link StandardContext} used by {@link TomcatWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TomcatEmbeddedContext extends StandardContext {

	private TomcatStarter starter;

	private MimeMappings mimeMappings;

	/**
     * Loads the specified containers on startup.
     * 
     * @param children an array of containers to be loaded
     * @return true if the containers were successfully loaded, false otherwise
     */
    @Override
	public boolean loadOnStartup(Container[] children) {
		// deferred until later (see deferredLoadOnStartup)
		return true;
	}

	/**
     * Sets the manager for this TomcatEmbeddedContext.
     * 
     * @param manager the manager to be set
     */
    @Override
	public void setManager(Manager manager) {
		if (manager instanceof ManagerBase) {
			manager.setSessionIdGenerator(new LazySessionIdGenerator());
		}
		super.setManager(manager);
	}

	/**
     * Loads the specified wrappers on startup in a deferred manner.
     * This method is called during the startup process of the TomcatEmbeddedContext class.
     * It uses the class loader of the current thread context to load the wrappers.
     * 
     * @see TomcatEmbeddedContext
     * @see #getLoader()
     * @see #getClassLoader()
     * @see #findChildren()
     * @see #getLoadOnStartupWrappers(List)
     * @see #load(Object)
     */
    void deferredLoadOnStartup() {
		doWithThreadContextClassLoader(getLoader().getClassLoader(),
				() -> getLoadOnStartupWrappers(findChildren()).forEach(this::load));
	}

	/**
     * Returns a stream of Wrapper objects representing the children Containers with a non-negative load-on-startup value,
     * grouped by their load-on-startup value in ascending order.
     *
     * @param children an array of Container objects representing the children of the current Container
     * @return a Stream of Wrapper objects representing the children Containers with a non-negative load-on-startup value,
     *         grouped by their load-on-startup value in ascending order
     */
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

	/**
     * Loads the given wrapper.
     * 
     * @param wrapper the wrapper to be loaded
     * @throws WebServerException if an exception occurs while loading the wrapper and the computedFailCtxIfServletStartFails flag is set
     */
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
	private void doWithThreadContextClassLoader(ClassLoader classLoader, Runnable code) {
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
     * Sets the TomcatStarter for this TomcatEmbeddedContext.
     * 
     * @param starter the TomcatStarter to set
     */
    void setStarter(TomcatStarter starter) {
		this.starter = starter;
	}

	/**
     * Returns the starter object associated with this TomcatEmbeddedContext.
     *
     * @return the starter object associated with this TomcatEmbeddedContext
     */
    TomcatStarter getStarter() {
		return this.starter;
	}

	/**
     * Sets the MIME mappings for this TomcatEmbeddedContext.
     * 
     * @param mimeMappings the MIME mappings to be set
     */
    void setMimeMappings(MimeMappings mimeMappings) {
		this.mimeMappings = mimeMappings;
	}

	/**
     * Returns an array of all the MIME mappings found in the TomcatEmbeddedContext, including the ones inherited from the superclass.
     * 
     * @return an array of strings representing the MIME mappings
     */
    @Override
	public String[] findMimeMappings() {
		List<String> mappings = new ArrayList<>();
		mappings.addAll(Arrays.asList(super.findMimeMappings()));
		if (this.mimeMappings != null) {
			this.mimeMappings.forEach((mapping) -> mappings.add(mapping.getExtension()));
		}
		return mappings.toArray(String[]::new);
	}

	/**
     * Finds the MIME mapping for a given file extension.
     * 
     * @param extension the file extension for which to find the MIME mapping
     * @return the MIME mapping for the given file extension, or null if not found
     */
    @Override
	public String findMimeMapping(String extension) {
		String mimeMapping = super.findMimeMapping(extension);
		if (mimeMapping != null) {
			return mimeMapping;
		}
		return (this.mimeMappings != null) ? this.mimeMappings.get(extension) : null;
	}

}
