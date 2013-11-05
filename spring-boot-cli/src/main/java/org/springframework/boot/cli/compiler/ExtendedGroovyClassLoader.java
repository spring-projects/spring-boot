/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import groovy.lang.GroovyClassLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Extension of the {@link GroovyClassLoader} with support for obtaining '.class' files as
 * resources.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
class ExtendedGroovyClassLoader extends GroovyClassLoader {

	private static final String SHARED_PACKAGE = "org.springframework.boot.groovy";

	private final Map<String, byte[]> classResources = new HashMap<String, byte[]>();

	private final GroovyCompilerScope scope;

	private final CompilerConfiguration configuration;

	public ExtendedGroovyClassLoader(GroovyCompilerScope scope) {
		this(scope, createParentClassLoader(scope), new CompilerConfiguration());
	}

	private static ClassLoader createParentClassLoader(GroovyCompilerScope scope) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (scope == GroovyCompilerScope.DEFAULT) {
			classLoader = new DefaultScopeParentClassLoader(classLoader);
		}
		return classLoader;
	}

	private ExtendedGroovyClassLoader(GroovyCompilerScope scope, ClassLoader parent,
			CompilerConfiguration configuration) {
		super(parent, configuration);
		this.configuration = configuration;
		this.scope = scope;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return super.findClass(name);
		}
		catch (ClassNotFoundException ex) {
			if (this.scope == GroovyCompilerScope.DEFAULT
					&& name.startsWith(SHARED_PACKAGE)) {
				Class<?> sharedClass = findSharedClass(name);
				if (sharedClass != null) {
					return sharedClass;
				}
			}
			throw ex;
		}
	}

	private Class<?> findSharedClass(String name) {
		try {
			String path = name.replace('.', '/').concat(".class");
			InputStream inputStream = getParent().getResourceAsStream(path);
			if (inputStream != null) {
				try {
					return defineClass(name, FileCopyUtils.copyToByteArray(inputStream));
				}
				finally {
					inputStream.close();
				}
			}
			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		InputStream resourceStream = super.getResourceAsStream(name);
		if (resourceStream == null) {
			byte[] bytes = this.classResources.get(name);
			resourceStream = bytes == null ? null : new ByteArrayInputStream(bytes);
		}
		return resourceStream;
	}

	@Override
	public ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
		InnerLoader loader = AccessController
				.doPrivileged(new PrivilegedAction<InnerLoader>() {
					@Override
					public InnerLoader run() {
						return new InnerLoader(ExtendedGroovyClassLoader.this);
					}
				});
		return new ExtendedClassCollector(loader, unit, su);
	}

	public CompilerConfiguration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Inner collector class used to track as classes are added.
	 */
	protected class ExtendedClassCollector extends ClassCollector {

		protected ExtendedClassCollector(InnerLoader loader, CompilationUnit unit,
				SourceUnit su) {
			super(loader, unit, su);
		}

		@Override
		protected Class<?> createClass(byte[] code, ClassNode classNode) {
			Class<?> createdClass = super.createClass(code, classNode);
			ExtendedGroovyClassLoader.this.classResources.put(classNode.getName()
					.replace(".", "/") + ".class", code);
			return createdClass;
		}
	}

	/**
	 * ClassLoader used for a parent that filters so that only classes from groovy-all.jar
	 * are exposed.
	 */
	private static class DefaultScopeParentClassLoader extends ClassLoader {

		private final URLClassLoader groovyOnlyClassLoader;

		public DefaultScopeParentClassLoader(ClassLoader parent) {
			super(parent);
			this.groovyOnlyClassLoader = new URLClassLoader(
					new URL[] { getGroovyJar(parent) }, null);
		}

		private URL getGroovyJar(final ClassLoader parent) {
			URL result = findGroovyJarDirectly(parent);
			if (result == null) {
				result = findGroovyJarFromClassPath(parent);
			}
			Assert.state(result != null, "Unable to find groovy JAR");
			return result;
		}

		private URL findGroovyJarDirectly(ClassLoader classLoader) {
			while (classLoader != null) {
				if (classLoader instanceof URLClassLoader) {
					URL[] urls = ((URLClassLoader) classLoader).getURLs();
					for (URL url : urls) {
						if (isGroovyJar(url.toString())) {
							return url;
						}
					}
				}
				classLoader = classLoader.getParent();
			}
			return null;
		}

		private URL findGroovyJarFromClassPath(ClassLoader parent) {
			String classpath = System.getProperty("java.class.path");
			String[] entries = classpath.split(System.getProperty("path.separator"));
			for (String entry : entries) {
				if (isGroovyJar(entry)) {
					File file = new File(entry);
					if (file.canRead()) {
						try {
							return file.toURI().toURL();
						}
						catch (MalformedURLException ex) {
							// Swallow and continue
						}
					}
				}
			}
			return null;
		}

		private boolean isGroovyJar(String entry) {
			return entry.contains("/groovy-all");
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			this.groovyOnlyClassLoader.loadClass(name);
			return super.loadClass(name, resolve);
		}
	}

}
