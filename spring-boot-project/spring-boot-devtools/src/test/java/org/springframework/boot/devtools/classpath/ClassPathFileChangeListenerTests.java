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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ClassPathFileChangeListener}.
 *
 * @author Phillip Webb
 */
class ClassPathFileChangeListenerTests {

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private ClassPathRestartStrategy restartStrategy;

	@Mock
	private FileSystemWatcher fileSystemWatcher;

	@Captor
	private ArgumentCaptor<ApplicationEvent> eventCaptor;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void eventPublisherMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ClassPathFileChangeListener(null, this.restartStrategy, this.fileSystemWatcher))
				.withMessageContaining("EventPublisher must not be null");
	}

	@Test
	void restartStrategyMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ClassPathFileChangeListener(this.eventPublisher, null, this.fileSystemWatcher))
				.withMessageContaining("RestartStrategy must not be null");
	}

	@Test
	void sendsEventWithoutRestart() {
		testSendsEvent(false);
		verify(this.fileSystemWatcher, never()).stop();
	}

	@Test
	void sendsEventWithRestart() {
		testSendsEvent(true);
		verify(this.fileSystemWatcher).stop();
	}

	private void testSendsEvent(boolean restart) {
		ClassPathFileChangeListener listener = new ClassPathFileChangeListener(this.eventPublisher,
				this.restartStrategy, this.fileSystemWatcher);
		File directory = new File("s1");
		File file = new File("f1");
		ChangedFile file1 = new ChangedFile(directory, file, ChangedFile.Type.ADD);
		ChangedFile file2 = new ChangedFile(directory, file, ChangedFile.Type.ADD);
		Set<ChangedFile> files = new LinkedHashSet<>();
		files.add(file1);
		files.add(file2);
		ChangedFiles changedFiles = new ChangedFiles(new File("source"), files);
		Set<ChangedFiles> changeSet = Collections.singleton(changedFiles);
		if (restart) {
			given(this.restartStrategy.isRestartRequired(file2)).willReturn(true);
		}
		listener.onChange(changeSet);
		verify(this.eventPublisher).publishEvent(this.eventCaptor.capture());
		ClassPathChangedEvent actualEvent = (ClassPathChangedEvent) this.eventCaptor.getValue();
		assertThat(actualEvent.getChangeSet()).isEqualTo(changeSet);
		assertThat(actualEvent.isRestartRequired()).isEqualTo(restart);
	}

}
