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

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.developertools.filewatch.ChangedFile;
import org.springframework.boot.developertools.filewatch.ChangedFiles;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ClassPathFileChangeListener}.
 *
 * @author Phillip Webb
 */
public class ClassPathFileChangeListenerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Captor
	private ArgumentCaptor<ApplicationEvent> eventCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void eventPublisherMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("EventPublisher must not be null");
		new ClassPathFileChangeListener(null, mock(ClassPathRestartStrategy.class));
	}

	@Test
	public void restartStrategyMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RestartStrategy must not be null");
		new ClassPathFileChangeListener(mock(ApplicationEventPublisher.class), null);
	}

	@Test
	public void sendsEventWithoutRestart() throws Exception {
		testSendsEvent(false);
	}

	@Test
	public void sendsEventWithRestart() throws Exception {
		testSendsEvent(true);
	}

	private void testSendsEvent(boolean restart) {
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		ClassPathRestartStrategy restartStrategy = mock(ClassPathRestartStrategy.class);
		ClassPathFileChangeListener listener = new ClassPathFileChangeListener(
				eventPublisher, restartStrategy);
		File folder = new File("s1");
		File file = new File("f1");
		ChangedFile file1 = new ChangedFile(folder, file, ChangedFile.Type.ADD);
		ChangedFile file2 = new ChangedFile(folder, file, ChangedFile.Type.ADD);
		Set<ChangedFile> files = new LinkedHashSet<ChangedFile>();
		files.add(file1);
		files.add(file2);
		ChangedFiles changedFiles = new ChangedFiles(new File("source"), files);
		Set<ChangedFiles> changeSet = Collections.singleton(changedFiles);
		if (restart) {
			given(restartStrategy.isRestartRequired(file2)).willReturn(true);
		}
		listener.onChange(changeSet);
		verify(eventPublisher).publishEvent(this.eventCaptor.capture());
		ClassPathChangedEvent actualEvent = (ClassPathChangedEvent) this.eventCaptor
				.getValue();
		assertThat(actualEvent.getChangeSet(), equalTo(changeSet));
		assertThat(actualEvent.isRestartRequired(), equalTo(restart));
	}

}
