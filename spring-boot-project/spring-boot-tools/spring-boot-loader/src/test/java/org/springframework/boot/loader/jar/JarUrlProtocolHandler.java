/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * JUnit 5 {@link Extension} for tests that interact with Spring Boot's {@link Handler}
 * for {@code jar:} URLs. Ensures that the handler is registered prior to test execution
 * and cleans up the handler's root file cache afterwards.
 *
 * @author Andy Wilkinson
 */
class JarUrlProtocolHandler implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		JarFile.registerUrlProtocolHandler();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterEach(ExtensionContext context) throws Exception {
		Map<File, JarFile> rootFileCache = ((SoftReference<Map<File, JarFile>>) ReflectionTestUtils
				.getField(Handler.class, "rootFileCache")).get();
		if (rootFileCache != null) {
			for (JarFile rootJarFile : rootFileCache.values()) {
				rootJarFile.close();
			}
			rootFileCache.clear();
		}
	}

}
