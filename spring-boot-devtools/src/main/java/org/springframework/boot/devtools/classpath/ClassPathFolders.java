/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.classpath;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ResourceUtils;

/**
 * Provides access to entries on the classpath that refer to folders.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class ClassPathFolders implements Iterable<File> {

	private static final Log logger = LogFactory.getLog(ClassPathFolders.class);

	private final List<File> folders = new ArrayList<File>();

	public ClassPathFolders(URL[] urls) {
		if (urls != null) {
			addUrls(urls);
		}
	}

	private void addUrls(URL[] urls) {
		for (URL url : urls) {
			addUrl(url);
		}
	}

	private void addUrl(URL url) {
		if (url.getProtocol().equals("file") && url.getPath().endsWith("/")) {
			try {
				this.folders.add(ResourceUtils.getFile(url));
			}
			catch (Exception ex) {
				logger.warn("Unable to get classpath URL " + url);
				logger.trace("Unable to get classpath URL " + url, ex);
			}
		}
	}

	@Override
	public Iterator<File> iterator() {
		return Collections.unmodifiableList(this.folders).iterator();
	}

}
