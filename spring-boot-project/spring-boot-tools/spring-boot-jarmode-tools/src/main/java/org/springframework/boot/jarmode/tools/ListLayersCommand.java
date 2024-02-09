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

import org.springframework.boot.jarmode.tools.Layers.LayersNotEnabledException;

/**
 * The {@code 'list-layers'} tools command.
 *
 * @author Moritz Halbritter
 */
class ListLayersCommand extends Command {

	private final Context context;

	ListLayersCommand(Context context) {
		super("list-layers", "List layers from the jar that can be extracted", Options.none(), Parameters.none());
		this.context = context;
	}

	@Override
	void run(PrintStream out, Map<Option, String> options, List<String> parameters) {
		try {
			Layers layers = Layers.get(this.context);
			printLayers(out, layers);
		}
		catch (LayersNotEnabledException ex) {
			printError(out, "Layers are not enabled");
		}
	}

	void printLayers(PrintStream out, Layers layers) {
		layers.forEach(out::println);
	}

	private void printError(PrintStream out, String message) {
		out.println("Error: " + message);
		out.println();
	}

}
