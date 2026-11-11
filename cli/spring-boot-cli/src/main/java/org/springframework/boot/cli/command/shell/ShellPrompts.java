/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.shell;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Abstraction to manage a stack of prompts.
 *
 * @author Phillip Webb
 * @since 1.0.0
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
