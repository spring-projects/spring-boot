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

package org.springframework.boot.devtools.classpath;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Encapsulates a {@link FileSystemWatcher} to watch the local classpath folders for
 * changes.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassPathFileChangeListener
 */
public class ClassPathFileSystemWatcher implements InitializingBean, DisposableBean,
		ApplicationContextAware {

	private static final Log logger = LogFactory.getLog(ClassPathFileSystemWatcher.class);

	private final FileSystemWatcher fileSystemWatcher;

	private ClassPathRestartStrategy restartStrategy;

	private ApplicationContext applicationContext;

	/**
	 * Create a new {@link ClassPathFileSystemWatcher} instance.
	 * @param urls the classpath URLs to watch
	 */
	public ClassPathFileSystemWatcher(URL[] urls) {
		this(new FileSystemWatcher(), null, urls);
	}

	/**
	 * Create a new {@link ClassPathFileSystemWatcher} instance.
	 * @param restartStrategy the classpath restart strategy
	 * @param urls the URLs to watch
	 */
	public ClassPathFileSystemWatcher(ClassPathRestartStrategy restartStrategy, URL[] urls) {
		this(new FileSystemWatcher(), restartStrategy, urls);
	}

	/**
	 * Create a new {@link ClassPathFileSystemWatcher} instance.
	 * @param fileSystemWatcher the underlying {@link FileSystemWatcher} used to monitor
	 * the local file system
	 * @param restartStrategy the classpath restart strategy
	 * @param urls the URLs to watch
	 */
	public ClassPathFileSystemWatcher(FileSystemWatcher fileSystemWatcher,
			ClassPathRestartStrategy restartStrategy, URL[] urls) {
		Assert.notNull(fileSystemWatcher, "FileSystemWatcher must not be null");
		Assert.notNull(urls, "Urls must not be null");
		this.fileSystemWatcher = fileSystemWatcher;
		this.restartStrategy = restartStrategy;
		addUrls(urls);
	}

	private void addUrls(URL[] urls) {
		for (URL url : urls) {
			addUrl(url);
		}
	}

	private void addUrl(URL url) {
		if (url.getProtocol().equals("file") && url.getPath().endsWith("/")) {
			try {
				this.fileSystemWatcher.addSourceFolder(ResourceUtils.getFile(url));
			}
			catch (Exception ex) {
				logger.warn("Unable to watch classpath URL " + url);
				logger.trace("Unable to watch classpath URL " + url, ex);
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.restartStrategy != null) {
			this.fileSystemWatcher.addListener(new ClassPathFileChangeListener(
					this.applicationContext, this.restartStrategy));
		}
		this.fileSystemWatcher.start();
	}

	@Override
	public void destroy() throws Exception {
		this.fileSystemWatcher.stop();
	}

}
