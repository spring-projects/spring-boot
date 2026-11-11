/*
 * Copyright 2012-present the original author or authors.
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
import org.jspecify.annotations.Nullable;

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

	public ClassPathDirectories(URL @Nullable [] urls) {
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
				this.directories.add(ResourceUtils.getFile(url));
			}
			catch (Exception ex) {
				logger.warn(LogMessage.format("Unable to get classpath URL %s", url));
				logger.trace(LogMessage.format("Unable to get classpath URL %s", url), ex);
			}
		}
	}

	@Override
	public Iterator<File> iterator() {
		return Collections.unmodifiableList(this.directories).iterator();
	}

}
