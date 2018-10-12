/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.ManagerBase;

import org.springframework.util.Assert;
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
			((ManagerBase) manager).setSessionIdGenerator(new LazySessionIdGenerator());
		}
		super.setManager(manager);
	}

	public void deferredLoadOnStartup() throws LifecycleException {
		doWithThreadContextClassLoader(getLoader().getClassLoader(), () -> {
			boolean started = super.loadOnStartup(findChildren());
			Assert.state(started, "Unable to start embedded tomcat context " + getName());
		});
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
		ClassLoader existingLoader = (classLoader != null)
				? ClassUtils.overrideThreadContextClassLoader(classLoader) : null;
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
