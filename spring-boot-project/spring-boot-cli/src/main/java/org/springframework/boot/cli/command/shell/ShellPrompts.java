/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.command.shell;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Abstraction to manage a stack of prompts.
 *
 * @author Phillip Webb
 */
public class ShellPrompts {

	private static final String DEFAULT_PROMPT = "$ ";

	private final Deque<String> prompts = new ArrayDeque<>();

	/**
	 * Push a new prompt to be used by the shell.
	 * @param prompt the prompt
	 * @see #popPrompt()
	 */
	public void pushPrompt(String prompt) {
		this.prompts.push(prompt);
	}

	/**
	 * Pop a previously pushed prompt, returning to the previous value.
	 * @see #pushPrompt(String)
	 */
	public void popPrompt() {
		if (!this.prompts.isEmpty()) {
			this.prompts.pop();
		}
	}

	/**
	 * Returns the current prompt.
	 * @return the current prompt
	 */
	public String getPrompt() {
		return this.prompts.isEmpty() ? DEFAULT_PROMPT : this.prompts.peek();
	}

}
