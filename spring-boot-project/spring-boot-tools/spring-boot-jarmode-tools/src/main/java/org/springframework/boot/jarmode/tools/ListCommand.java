/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * The {@code 'list'} tools command.
 *
 * Delegates the actual work to {@link ListLayersCommand}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class ListCommand extends Command {

	private final ListLayersCommand delegate;

	ListCommand(Context context) {
		super("list", "List layers from the jar that can be extracted", Options.none(), Parameters.none());
		this.delegate = new ListLayersCommand(context);
	}

	@Override
	boolean isDeprecated() {
		return true;
	}

	@Override
	String getDeprecationMessage() {
		return "Use '-Djarmode=tools list-layers' instead.";
	}

	@Override
	void run(PrintStream out, Map<Option, String> options, List<String> parameters) {
		this.delegate.run(out, options, parameters);
	}

	void printLayers(Layers layers, PrintStream out) {
		this.delegate.printLayers(out, layers);
	}

}
