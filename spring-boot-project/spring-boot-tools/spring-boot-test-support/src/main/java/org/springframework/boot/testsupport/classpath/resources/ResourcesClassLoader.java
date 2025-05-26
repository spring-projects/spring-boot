/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testsupport.classpath.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.springframework.core.SmartClassLoader;

/**
 * A {@link ClassLoader} that provides access to {@link Resources resources}.
 *
 * @author Andy Wilkinson
 */
class ResourcesClassLoader extends ClassLoader implements SmartClassLoader {

	private final Resources resources;

	ResourcesClassLoader(ClassLoader parent, Resources resources) {
		super(parent);
		this.resources = resources;
	}

	@Override
	public URL getResource(String name) {
		Resource resource = this.resources.find(name);
		return (resource != null) ? urlOf(resource) : getParent().getResource(name);
	}

	private URL urlOf(Resource resource) {
		try {
			return resource.path().toUri().toURL();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Resource resource = this.resources.find(name);
		ArrayList<URL> urls = new ArrayList<>();
		if (resource != null) {
			URL resourceUrl = urlOf(resource);
			urls.add(resourceUrl);
		}
		if (resource == null || resource.additional()) {
			urls.addAll(Collections.list(getParent().getResources(name)));
		}
		return Collections.enumeration(urls);
	}

	@Override
	public ClassLoader getOriginalClassLoader() {
		return getParent();
	}

	@Override
	public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
		return defineClass(name, b, 0, b.length, protectionDomain);
	}

}
