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

package org.springframework.boot.buildpack.platform.build;

import java.io.PrintStream;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.TotalProgressBar;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;

/**
 * {@link BuildLog} implementation that prints output to a {@link PrintStream}.
 *
 * @author Phillip Webb
 * @see BuildLog#to(PrintStream)
 */
class PrintStreamBuildLog extends AbstractBuildLog {

	private final PrintStream out;

	PrintStreamBuildLog(PrintStream out) {
		this.out = out;
	}

	@Override
	protected void log(String message) {
		this.out.println(message);
	}

	@Override
	protected Consumer<TotalProgressEvent> getProgressConsumer(String prefix) {
		return new TotalProgressBar(prefix, '.', false, this.out);
	}

}
