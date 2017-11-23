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

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Allows to watch for changes in source directories
 * 
 * @author Damian Suchodolski
 *
 */
public interface FileWatcher {

	/**
	 * Add listener for file change events. Cannot be called after the watcher has been
	 * {@link #start() started}.
	 * @param fileChangeListener the listener to add
	 */
	void addListener(FileChangeListener fileChangeListener);

	/**
	 * Add source folders to monitor. Cannot be called after the watcher has been
	 * {@link #start() started}.
	 * @param folders the folders to monitor
	 */
	void addSourceFolders(Iterable<File> folders);

	/**
	 * Add a source folder to monitor. Cannot be called after the watcher has been
	 * {@link #start() started}.
	 * @param folder the folder to monitor
	 */
	void addSourceFolder(File folder);

	/**
	 * Set an optional {@link FileFilter} used to limit the files that trigger a change.
	 * @param triggerFilter a trigger filter or null
	 */
	void setTriggerFilter(FileFilter triggerFilter);

	/**
	 * Start monitoring the source folder for changes.
	 * @throws IOException
	 */
	void start();

	/**
	 * Stop monitoring the source folders.
	 */
	void stop();

}