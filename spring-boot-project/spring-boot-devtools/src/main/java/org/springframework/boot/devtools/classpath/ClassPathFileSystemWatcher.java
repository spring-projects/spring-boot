/*
 * Copyright 2012-2022 the original author or authors.
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

import java.net.URL;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.filewatch.FileSystemWatcherFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * Encapsulates a {@link FileSystemWatcher} to watch the local classpath directories for
 * changes.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassPathFileChangeListener
 */
public class ClassPathFileSystemWatcher implements InitializingBean, DisposableBean, ApplicationContextAware {

	private final FileSystemWatcher fileSystemWatcher;

	private final ClassPathRestartStrategy restartStrategy;

	private ApplicationContext applicationContext;

	private boolean stopWatcherOnRestart;

	/**
	 * Create a new {@link ClassPathFileSystemWatcher} instance.
	 * @param fileSystemWatcherFactory a factory to create the underlying
	 * {@link FileSystemWatcher} used to monitor the local file system
	 * @param restartStrategy the classpath restart strategy
	 * @param urls the URLs to watch
	 */
	public ClassPathFileSystemWatcher(FileSystemWatcherFactory fileSystemWatcherFactory,
			ClassPathRestartStrategy restartStrategy, URL[] urls) {
		Assert.notNull(fileSystemWatcherFactory, "FileSystemWatcherFactory must not be null");
		Assert.notNull(urls, "Urls must not be null");
		this.fileSystemWatcher = fileSystemWatcherFactory.getFileSystemWatcher();
		this.restartStrategy = restartStrategy;
		this.fileSystemWatcher.addSourceDirectories(new ClassPathDirectories(urls));
	}

	/**
	 * Set if the {@link FileSystemWatcher} should be stopped when a full restart occurs.
	 * @param stopWatcherOnRestart if the watcher should be stopped when a restart occurs
	 */
	public void setStopWatcherOnRestart(boolean stopWatcherOnRestart) {
		this.stopWatcherOnRestart = stopWatcherOnRestart;
	}

	/**
     * Sets the application context for this ClassPathFileSystemWatcher.
     * 
     * @param applicationContext the application context to be set
     * @throws BeansException if an error occurs while setting the application context
     */
    @Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
     * This method is called after all the properties have been set for the ClassPathFileSystemWatcher object.
     * It starts the file system watcher and adds a listener to it if a restart strategy is provided.
     * If the stopWatcherOnRestart flag is set to true, it stops the watcher before adding the listener.
     * The listener is an instance of ClassPathFileChangeListener which is responsible for handling file change events.
     * 
     * @throws Exception if an error occurs while setting up the file system watcher or adding the listener
     */
    @Override
	public void afterPropertiesSet() throws Exception {
		if (this.restartStrategy != null) {
			FileSystemWatcher watcherToStop = null;
			if (this.stopWatcherOnRestart) {
				watcherToStop = this.fileSystemWatcher;
			}
			this.fileSystemWatcher.addListener(
					new ClassPathFileChangeListener(this.applicationContext, this.restartStrategy, watcherToStop));
		}
		this.fileSystemWatcher.start();
	}

	/**
     * Stops the file system watcher.
     * 
     * @throws Exception if an error occurs while stopping the file system watcher
     */
    @Override
	public void destroy() throws Exception {
		this.fileSystemWatcher.stop();
	}

}
