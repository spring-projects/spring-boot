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

package org.springframework.boot.cli.compiler.grape;

import java.io.PrintStream;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Provide detailed progress feedback for long running resolves.
 *
 * @author Andy Wilkinson
 */
final class DetailedProgressReporter implements ProgressReporter {

	DetailedProgressReporter(DefaultRepositorySystemSession session,
			final PrintStream out) {

		session.setTransferListener(new AbstractTransferListener() {

			@Override
			public void transferStarted(TransferEvent event)
					throws TransferCancelledException {
				out.println("Downloading: " + getResourceIdentifier(event.getResource()));
			}

			@Override
			public void transferSucceeded(TransferEvent event) {
				out.printf("Downloaded: %s (%s)%n",
						getResourceIdentifier(event.getResource()),
						getTransferSpeed(event));
			}
		});
	}

	private String getResourceIdentifier(TransferResource resource) {
		return resource.getRepositoryUrl() + resource.getResourceName();
	}

	private String getTransferSpeed(TransferEvent event) {
		long kb = event.getTransferredBytes() / 1024;
		float seconds = (System.currentTimeMillis()
				- event.getResource().getTransferStartTime()) / 1000.0f;

		return String.format("%dKB at %.1fKB/sec", kb, (kb / seconds));
	}

	@Override
	public void finished() {
	}

}
