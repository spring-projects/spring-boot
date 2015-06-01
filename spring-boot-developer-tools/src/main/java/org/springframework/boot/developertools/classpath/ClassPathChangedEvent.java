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

package org.springframework.boot.developertools.classpath;

import java.util.Set;

import org.springframework.boot.developertools.filewatch.ChangedFiles;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * {@link ApplicationEvent} containing details of a classpath change.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassPathFileChangeListener
 */
public class ClassPathChangedEvent extends ApplicationEvent {

	private final Set<ChangedFiles> changeSet;

	private final boolean restartRequired;

	/**
	 * Create a new {@link ClassPathChangedEvent}.
	 * @param source the source of the event
	 * @param changeSet the changed files
	 * @param restartRequired if a restart is required due to the change
	 */
	public ClassPathChangedEvent(Object source, Set<ChangedFiles> changeSet,
			boolean restartRequired) {
		super(source);
		Assert.notNull(changeSet, "ChangeSet must not be null");
		this.changeSet = changeSet;
		this.restartRequired = restartRequired;
	}

	/**
	 * Return details of the files that changed.
	 * @return the changed files
	 */
	public Set<ChangedFiles> getChangeSet() {
		return this.changeSet;
	}

	/**
	 * Return if an application restart is required due to the change.
	 * @return if an application restart is required
	 */
	public boolean isRestartRequired() {
		return this.restartRequired;
	}

}
