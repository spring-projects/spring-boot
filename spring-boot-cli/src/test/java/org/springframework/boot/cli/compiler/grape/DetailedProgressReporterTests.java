/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DetailedProgressReporter}.
 *
 * @author Andy Wilkinson
 */
public final class DetailedProgressReporterTests {

	private static final String REPOSITORY = "http://my.repository.com/";

	private static final String ARTIFACT = "org/alpha/bravo/charlie/1.2.3/charlie-1.2.3.jar";

	private final TransferResource resource = new TransferResource(REPOSITORY, ARTIFACT,
			null, null);

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

	private final PrintStream out = new PrintStream(this.baos);

	private final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

	@Before
	public void initialize() {
		new DetailedProgressReporter(this.session, this.out);
	}

	@Test
	public void downloading() throws TransferCancelledException {
		TransferEvent startedEvent = new TransferEvent.Builder(this.session,
				this.resource).build();
		this.session.getTransferListener().transferStarted(startedEvent);

		assertEquals(String.format("Downloading: %s%s%n", REPOSITORY, ARTIFACT),
				new String(this.baos.toByteArray()));
	}

	@Test
	public void downloaded() throws InterruptedException {
		// Ensure some transfer time
		Thread.sleep(100);
		TransferEvent completedEvent = new TransferEvent.Builder(this.session,
				this.resource).addTransferredBytes(4096).build();
		this.session.getTransferListener().transferSucceeded(completedEvent);
		String message = new String(this.baos.toByteArray()).replace("\\", "/");
		assertThat(message, startsWith("Downloaded: " + REPOSITORY + ARTIFACT));
		assertThat(message, containsString("4KB at"));
		assertThat(message, containsString("KB/sec"));
		assertThat(message, endsWith("\n"));
	}
}
