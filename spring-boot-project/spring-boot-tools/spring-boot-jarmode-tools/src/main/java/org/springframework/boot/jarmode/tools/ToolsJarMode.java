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

import org.springframework.boot.loader.jarmode.JarMode;

/**
 * {@link JarMode} providing {@code "tools"} support.
 *
 * @author Moritz Halbritter
 * @since 3.3.0
 */
public class ToolsJarMode implements JarMode {

	private final Context context;

	private final PrintStream out;

	public ToolsJarMode() {
		this(null, null);
	}

	public ToolsJarMode(Context context, PrintStream out) {
		this.context = (context != null) ? context : new Context();
		this.out = (out != null) ? out : System.out;
	}

	@Override
	public boolean accepts(String mode) {
		return "tools".equalsIgnoreCase(mode);
	}

	@Override
	public void run(String mode, String[] args) {
		try {
			new Runner(this.out, this.context, getCommands(this.context)).run(args);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	static List<Command> getCommands(Context context) {
		return List.of(new ExtractCommand(context), new ListLayersCommand(context));
	}

}
