/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.session.ManagerBase;

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

	public void deferredLoadOnStartup() throws LifecycleException {
		doWithThreadContextClassLoader(getLoader().getClassLoader(),
				() -> getLoadOnStartupWrappers(findChildren()).forEach(this::load));
	}

	private Stream<Wrapper> getLoadOnStartupWrappers(Container[] children) {
		Map<Integer, List<Wrapper>> grouped = new TreeMap<>();
		for (Container child : children) {
			Wrapper wrapper = (Wrapper) child;
			int order = wrapper.getLoadOnStartup();
			if (order >= 0) {
				grouped.computeIfAbsent(order, ArrayList::new);
				grouped.get(order).add(wrapper);
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
	 * loadOnStartup in what we think its going to be the main webapp classloader at
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

	public void setStarter(TomcatStarter starter) {
		this.starter = starter;
	}

	public TomcatStarter getStarter() {
		return this.starter;
	}

}
