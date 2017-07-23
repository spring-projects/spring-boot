/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Set;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link JarScanner} decorator allowing alternative default jar pattern matching. This
 * class extends {@link StandardJarScanner} rather than implementing the
 * {@link JarScanner} due to API changes introduced in Tomcat 8.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see #apply(TomcatEmbeddedContext, Set)
 */
class SkipPatternJarScanner extends StandardJarScanner {

	private final JarScanner jarScanner;

	SkipPatternJarScanner(JarScanner jarScanner, Set<String> patterns) {
		Assert.notNull(jarScanner, "JarScanner must not be null");
		Assert.notNull(patterns, "Patterns must not be null");
		this.jarScanner = jarScanner;
		StandardJarScanFilter filter = new StandardJarScanFilter();
		filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(patterns));
		this.jarScanner.setJarScanFilter(filter);
	}

	/**
	 * Apply this decorator the specified context.
	 * @param context the context to apply to
	 * @param patterns the jar skip patterns or {@code null} for defaults
	 */
	static void apply(TomcatEmbeddedContext context, Set<String> patterns) {
		SkipPatternJarScanner scanner = new SkipPatternJarScanner(context.getJarScanner(),
				patterns);
		context.setJarScanner(scanner);
	}

}
