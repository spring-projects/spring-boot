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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.naming.resources.FileDirContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstraction to add resources that works with both Tomcat 8 and 7.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
abstract class TomcatResources {

	private final Context context;

	public TomcatResources(Context context) {
		this.context = context;
	}

	/**
	 * Add resources from the classpath
	 */
	public void addClasspathResources() {
		ClassLoader loader = getClass().getClassLoader();
		if (loader instanceof URLClassLoader) {
			for (URL url : ((URLClassLoader) loader).getURLs()) {
				String file = url.getFile();
				if (file.endsWith(".jar") || file.endsWith(".jar!/")) {
					String jar = url.toString();
					if (!jar.startsWith("jar:")) {
						// A jar file in the file system. Convert to Jar URL.
						jar = "jar:" + jar + "!/";
					}
					addJar(jar);
				}
				else if (url.toString().startsWith("file:")) {
					String dir = url.toString().substring("file:".length());
					if (new File(dir).isDirectory()) {
						addDir(dir, url);
					}
				}
			}
		}
	}

	protected final Context getContext() {
		return this.context;
	}

	/**
	 * Called to add a JAR to the resources.
	 * @param jar the URL spec for the jar
	 */
	protected abstract void addJar(String jar);

	/**
	 * Called to add a dir to the resource.
	 * @param dir the dir
	 * @param url the URL
	 */
	protected abstract void addDir(String dir, URL url);

	/**
	 * Return a {@link TomcatResources} instance for the currently running Tomcat version.
	 * @param context the tomcat context
	 * @return a {@link TomcatResources} instance.
	 */
	public static TomcatResources get(Context context) {
		if (ClassUtils.isPresent("org.apache.catalina.deploy.ErrorPage", null)) {
			return new Tomcat7Resources(context);
		}
		return new Tomcat8Resources(context);
	}

	/**
	 * {@link TomcatResources} for Tomcat 7.
	 */
	private static class Tomcat7Resources extends TomcatResources {

		public Tomcat7Resources(Context context) {
			super(context);
		}

		@Override
		protected void addJar(String jar) {
			try {
				getContext().addResourceJarUrl(new URL(jar));
			}
			catch (MalformedURLException ex) {
				// Ignore?
			}
		}

		@Override
		protected void addDir(String dir, URL url) {
			if (getContext() instanceof ServletContext) {
				FileDirContext files = new FileDirContext();
				files.setDocBase(dir);
				((StandardContext) getContext()).addResourcesDirContext(files);
			}
		}

	}

	/**
	 * {@link TomcatResources} for Tomcat 8.
	 */
	static class Tomcat8Resources extends TomcatResources {

		private Object resources;

		private Method createWebResourceSetMethod;

		private Enum<?> resourceJarEnum;

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Tomcat8Resources(Context context) {
			super(context);
			try {
				this.resources = ReflectionUtils.findMethod(context.getClass(),
						"getResources").invoke(context);
				Class resourceSetType = ClassUtils.resolveClassName(
						"org.apache.catalina.WebResourceRoot.ResourceSetType", null);
				this.createWebResourceSetMethod = ReflectionUtils.findMethod(
						this.resources.getClass(), "createWebResourceSet",
						resourceSetType, String.class, URL.class, String.class);
				this.resourceJarEnum = Enum.valueOf(resourceSetType, "RESOURCE_JAR");
			}
			catch (Exception ex) {
				throw new IllegalStateException("Tomcat 8 reflection failed", ex);
			}
		}

		@Override
		protected void addJar(String jar) {
			addResourceSet(jar);
		}

		@Override
		protected void addDir(String dir, URL url) {
			addResourceSet(url.toString());
		}

		private void addResourceSet(String resource) {
			try {
				if (isInsideNestedJar(resource)) {
					// It's a nested jar but we now don't want the suffix because Tomcat
					// is going to try and locate it as a root URL (not the resource
					// inside it)
					resource = resource.substring(0, resource.length() - 2);
				}
				URL url = new URL(resource);
				String path = "/META-INF/resources";
				createWebResourceSet("/", url, path);
			}
			catch (Exception ex) {
				// Ignore (probably not a directory)
			}
		}

		private boolean isInsideNestedJar(String dir) {
			return dir.indexOf("!/") < dir.lastIndexOf("!/");
		}

		private void createWebResourceSet(String webAppMount, URL url, String path)
				throws Exception {
			this.createWebResourceSetMethod.invoke(this.resources, this.resourceJarEnum,
					webAppMount, url, path);
		}

	}

}
