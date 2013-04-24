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

package org.springframework.bootstrap.cli.compiler;

import groovy.lang.GroovyClassLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

/**
 * Extension of the {@link GroovyClassLoader} that support for obtaining '.class' files as
 * resources.
 * 
 * @author Phillip Webb
 */
class ExtendedGroovyClassLoader extends GroovyClassLoader {

	private Map<String, byte[]> classResources = new HashMap<String, byte[]>();

	public ExtendedGroovyClassLoader(ClassLoader loader, CompilerConfiguration config) {
		super(loader, config);
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
	protected ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
		InnerLoader loader = AccessController
				.doPrivileged(new PrivilegedAction<InnerLoader>() {
					@Override
					public InnerLoader run() {
						return new InnerLoader(ExtendedGroovyClassLoader.this);
					}
				});
		return new ExtendedClassCollector(loader, unit, su);
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

}
