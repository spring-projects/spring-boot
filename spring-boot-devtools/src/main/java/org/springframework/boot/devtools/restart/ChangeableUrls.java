/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A filtered collections of URLs which can be change after the application has started.
 *
 * @author Phillip Webb
 */
class ChangeableUrls implements Iterable<URL> {

	private static final String[] SKIPPED_PROJECTS = { "spring-boot",
			"spring-boot-devtools", "spring-boot-autoconfigure",
			"spring-boot-actuator", "spring-boot-starter" };

	private static final Pattern STARTER_PATTERN = Pattern
			.compile("\\/spring-boot-starter-[\\w-]+\\/");

	private final List<URL> urls;

	private ChangeableUrls(URL... urls) {
		List<URL> reloadableUrls = new ArrayList<URL>(urls.length);
		for (URL url : urls) {
			if (isReloadable(url)) {
				reloadableUrls.add(url);
			}
		}
		this.urls = Collections.unmodifiableList(reloadableUrls);
	}

	private boolean isReloadable(URL url) {
		String urlString = url.toString();
		return isFolderUrl(urlString) && !isSkipped(urlString);
	}

	private boolean isFolderUrl(String urlString) {
		return urlString.startsWith("file:") && urlString.endsWith("/");
	}

	private boolean isSkipped(String urlString) {
		// Skip certain spring-boot projects to allow them to be imported in the same IDE
		for (String skipped : SKIPPED_PROJECTS) {
			if (urlString.contains("/" + skipped + "/target/classes/")) {
				return true;
			}
		}
		// Skip all starter projects
		if (STARTER_PATTERN.matcher(urlString).find()) {
			return true;
		}
		return false;
	}

	@Override
	public Iterator<URL> iterator() {
		return this.urls.iterator();
	}

	public int size() {
		return this.urls.size();
	}

	public URL[] toArray() {
		return this.urls.toArray(new URL[this.urls.size()]);
	}

	public List<URL> toList() {
		return Collections.unmodifiableList(this.urls);
	}

	@Override
	public String toString() {
		return this.urls.toString();
	}

	public static ChangeableUrls fromUrlClassLoader(URLClassLoader classLoader) {
		return fromUrls(classLoader.getURLs());
	}

	public static ChangeableUrls fromUrls(Collection<URL> urls) {
		return fromUrls(new ArrayList<URL>(urls).toArray(new URL[urls.size()]));
	}

	public static ChangeableUrls fromUrls(URL... urls) {
		return new ChangeableUrls(urls);
	}

}
