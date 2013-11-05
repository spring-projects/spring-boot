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

import java.util.concurrent.TimeUnit;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;

/**
 * Provide console progress feedback for long running resolves.
 * 
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ProgressReporter {

	private static final long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(3);

	private static final long PROGRESS_DELAY = TimeUnit.SECONDS.toMillis(1);

	private long startTime = System.currentTimeMillis();

	private long lastProgressTime = System.currentTimeMillis();

	private boolean started;

	private boolean finished;

	public ProgressReporter(DefaultRepositorySystemSession session) {
		session.setTransferListener(new AbstractTransferListener() {
			@Override
			public void transferStarted(TransferEvent event)
					throws TransferCancelledException {
				reportProgress();
			}

			@Override
			public void transferProgressed(TransferEvent event)
					throws TransferCancelledException {
				reportProgress();
			}
		});

		session.setRepositoryListener(new AbstractRepositoryListener() {
			@Override
			public void artifactResolved(RepositoryEvent event) {
				reportProgress();
			}
		});
	}

	private void reportProgress() {
		if (!this.finished && System.currentTimeMillis() - this.startTime > INITIAL_DELAY) {
			if (!this.started) {
				this.started = true;
				System.out.print("Resolving dependencies..");
				this.lastProgressTime = System.currentTimeMillis();
			}
			else if (System.currentTimeMillis() - this.lastProgressTime > PROGRESS_DELAY) {
				System.out.print(".");
				this.lastProgressTime = System.currentTimeMillis();
			}
		}
	}

	public void finished() {
		if (this.started && !this.finished) {
			this.finished = true;
			System.out.println("");
		}
	}
}
