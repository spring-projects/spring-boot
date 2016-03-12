/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Tomcat {@link StandardContext} used by {@link TomcatEmbeddedServletContainer} to
 * support deferred initialization.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TomcatEmbeddedContext extends StandardContext {

	private TomcatStarter starter;

	private final boolean overrideLoadOnStart;

	TomcatEmbeddedContext() {
		this.overrideLoadOnStart = ReflectionUtils
				.findMethod(StandardContext.class, "loadOnStartup", Container[].class)
				.getReturnType() == boolean.class;
	}

	@Override
	public boolean loadOnStartup(Container[] children) {
		if (this.overrideLoadOnStart) {
			return true;
		}
		return super.loadOnStartup(children);
	}

	public void deferredLoadOnStartup() {
		// Some older Servlet frameworks (e.g. Struts, BIRT) use the Thread context class
		// loader to create servlet instances in this phase. If they do that and then try
		// to initialize them later the class loader may have changed, so wrap the call to
		// loadOnStartup in what we think its going to be the main webapp classloader at
		// runtime.
		ClassLoader classLoader = getLoader().getClassLoader();
		ClassLoader existingLoader = null;
		if (classLoader != null) {
			existingLoader = ClassUtils.overrideThreadContextClassLoader(classLoader);
		}

		if (this.overrideLoadOnStart) {
			// Earlier versions of Tomcat used a version that returned void. If that
			// version is used our overridden loadOnStart method won't have been called
			// and the original will have already run.
			super.loadOnStartup(findChildren());
		}
		if (existingLoader != null) {
			ClassUtils.overrideThreadContextClassLoader(existingLoader);
		}
	}

	public void setStarter(TomcatStarter starter) {
		this.starter = starter;
	}

	public TomcatStarter getStarter() {
		return this.starter;
	}

}
