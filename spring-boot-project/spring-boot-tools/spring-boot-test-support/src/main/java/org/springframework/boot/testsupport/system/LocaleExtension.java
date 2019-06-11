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

package org.springframework.boot.testsupport.system;

import java.util.Locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Internal JUnit 5 {@code @Extension} to override system locale for test execution.
 *
 * @author Ilya Lukyanovich
 */
public class LocaleExtension implements BeforeEachCallback, AfterEachCallback {

	private final Locale locale;

	private Locale defaultLocale;

	public LocaleExtension(Locale locale) {
		this.locale = locale;
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		this.defaultLocale = Locale.getDefault();
		Locale.setDefault(this.locale);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		Locale.setDefault(this.defaultLocale);
	}

}
