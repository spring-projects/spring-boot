/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.core.log.LogMessage;
import org.springframework.util.ResourceUtils;

/**
 * Provides access to entries on the classpath that refer to directories.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class ClassPathDirectories implements Iterable<File> {

	private static final Log logger = LogFactory.getLog(ClassPathDirectories.class);

	private final List<File> directories = new ArrayList<>();

	/**
     * Constructs a new ClassPathDirectories object with the specified array of URLs.
     * 
     * @param urls the array of URLs representing the directories to be added to the classpath
     */
    public ClassPathDirectories(URL[] urls) {
		if (urls != null) {
			addUrls(urls);
		}
	}

	/**
     * Adds an array of URLs to the class path directories.
     * 
     * @param urls the array of URLs to be added
     */
    private void addUrls(URL[] urls) {
		for (URL url : urls) {
			addUrl(url);
		}
	}

	/**
     * Adds a URL to the list of directories if it is a file URL and ends with a forward slash.
     * 
     * @param url the URL to be added
     */
    private void addUrl(URL url) {
		if (url.getProtocol().equals("file") && url.getPath().endsWith("/")) {
			try {
				this.directories.add(ResourceUtils.getFile(url));
			}
			catch (Exception ex) {
				logger.warn(LogMessage.format("Unable to get classpath URL %s", url));
				logger.trace(LogMessage.format("Unable to get classpath URL %s", url), ex);
			}
		}
	}

	/**
     * Returns an iterator over the directories in the ClassPathDirectories object.
     *
     * @return an iterator over the directories in the ClassPathDirectories object
     */
    @Override
	public Iterator<File> iterator() {
		return Collections.unmodifiableList(this.directories).iterator();
	}

}
