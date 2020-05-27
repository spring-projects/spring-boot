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

package org.springframework.boot.buildpack.platform.docker;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Utility to render a simple progress bar based on consumed {@link TotalProgressEvent}
 * objects.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class TotalProgressBar implements Consumer<TotalProgressEvent> {

	private final char progressChar;

	private final boolean bookend;

	private final PrintStream out;

	private int printed;

	/**
	 * Create a new {@link TotalProgressBar} instance.
	 * @param prefix the prefix to output
	 */
	public TotalProgressBar(String prefix) {
		this(prefix, System.out);
	}

	/**
	 * Create a new {@link TotalProgressBar} instance.
	 * @param prefix the prefix to output
	 * @param out the output print stream to use
	 */
	public TotalProgressBar(String prefix, PrintStream out) {
		this(prefix, '#', true, out);
	}

	/**
	 * Create a new {@link TotalProgressBar} instance.
	 * @param prefix the prefix to output
	 * @param progressChar the progress char to print
	 * @param bookend if bookends should be printed
	 * @param out the output print stream to use
	 */
	public TotalProgressBar(String prefix, char progressChar, boolean bookend, PrintStream out) {
		this.progressChar = progressChar;
		this.bookend = bookend;
		if (prefix != null && !prefix.isEmpty()) {
			out.print(prefix);
			out.print(" ");
		}
		if (bookend) {
			out.print("[ ");
		}
		this.out = out;
	}

	@Override
	public void accept(TotalProgressEvent event) {
		int percent = event.getPercent() / 2;
		while (this.printed < percent) {
			this.out.print(this.progressChar);
			this.printed++;
		}
		if (event.getPercent() == 100) {
			this.out.println(this.bookend ? " ]" : "");
		}
	}

}
