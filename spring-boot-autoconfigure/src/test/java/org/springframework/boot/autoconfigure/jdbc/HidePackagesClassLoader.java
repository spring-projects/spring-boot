/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Test {@link URLClassLoader} that hides configurable packages. No class in one of those
 * packages or sub-packages are visible.
 *
 * @author Stephane Nicoll
 */
final class HidePackagesClassLoader extends URLClassLoader {

	private final String[] hiddenPackages;

	HidePackagesClassLoader(String... hiddenPackages) {
		super(new URL[0], DataSourceAutoConfigurationTests.class.getClassLoader());
		this.hiddenPackages = hiddenPackages;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		for (String hiddenPackage : this.hiddenPackages) {
			if (name.startsWith(hiddenPackage)) {
				throw new ClassNotFoundException();
			}
		}
		return super.loadClass(name, resolve);
	}

}
