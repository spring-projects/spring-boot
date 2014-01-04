/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;

import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.springframework.boot.cli.SpringCli;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A shell command for Spring Boot. Drops the user into an event loop (REPL) where command
 * line completion and history are available without relying on OS shell features.
 * 
 * @author Jon Brisbin
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ShellCommand extends AbstractCommand {

	private static final Method PROCESS_BUILDER_INHERIT_IO_METHOD = ReflectionUtils
			.findMethod(ProcessBuilder.class, "inheritIO");

	private String defaultPrompt = "$ ";

	private SpringCli springCli;

	private String prompt = this.defaultPrompt;

	private Stack<String> prompts = new Stack<String>();

	public ShellCommand(SpringCli springCli) {
		super("shell", "Start a nested shell");
		this.springCli = springCli;
	}

	@Override
	public void run(String... args) throws Exception {
		enhance(this.springCli);

		InputStream sysin = System.in;
		PrintStream systemOut = System.out;
		PrintStream systemErr = System.err;

		ConsoleReader consoleReader = createConsoleReader();
		printBanner();

		PrintStream out = new PrintStream(new ConsoleReaderOutputStream(consoleReader));
		System.setIn(consoleReader.getInput());
		System.setOut(out);
		System.setErr(out);

		try {
			runReadLoop(consoleReader, systemOut, systemErr);
		}
		finally {
			System.setIn(sysin);
			System.setOut(systemOut);
			System.setErr(systemErr);
			consoleReader.shutdown();
		}
	}

	protected void enhance(SpringCli cli) {
		this.defaultPrompt = cli.getDisplayName().trim() + "> ";
		this.prompt = this.defaultPrompt;
		cli.setDisplayName("");

		RunCommand run = (RunCommand) cli.find("run");
		if (run != null) {
			StopCommand stop = new StopCommand(run);
			cli.register(stop);
		}

		PromptCommand prompt = new PromptCommand(this);
		cli.register(prompt);
		cli.register(cli.getInitCommand());
	}

	private ConsoleReader createConsoleReader() throws IOException {
		ConsoleReader reader = new ConsoleReader();
		reader.addCompleter(new CommandCompleter(reader, this.springCli));
		reader.setHistoryEnabled(true);
		reader.setCompletionHandler(new CandidateListCompletionHandler());
		return reader;
	}

	protected void printBanner() {
		String version = ShellCommand.class.getPackage().getImplementationVersion();
		version = (version == null ? "" : " (v" + version + ")");
		System.out.println("Spring Boot CLI" + version);
		System.out.println("Hit TAB to complete. Type 'help' and hit "
				+ "RETURN for help, and 'quit' to exit.");
	}

	private void runReadLoop(final ConsoleReader consoleReader,
			final PrintStream systemOut, final PrintStream systemErr) throws IOException {
		StringBuffer data = new StringBuffer();
		while (true) {
			String line = consoleReader.readLine(this.prompt);

			if (line == null || "quit".equals(line.trim()) || "exit".equals(line.trim())) {
				return;
			}

			if ("clear".equals(line.trim())) {
				consoleReader.clearScreen();
				continue;
			}

			if (line.contains("<<")) {
				int startMultiline = line.indexOf("<<");
				data.append(line.substring(startMultiline + 2));
				line = line.substring(0, startMultiline);
				readMultiLineData(consoleReader, data);
			}

			line = line.trim();
			boolean isLaunchProcessCommand = line.startsWith("!");
			if (isLaunchProcessCommand) {
				line = line.substring(1);
			}

			List<String> args = parseArgs(line);
			if (data.length() > 0) {
				args.add(data.toString());
				data.setLength(0);
			}
			if (args.size() > 0) {
				if (isLaunchProcessCommand) {
					launchProcess(args, systemOut, systemErr);
				}
				else {
					runCommand(args);
				}
			}
		}
	}

	private void readMultiLineData(final ConsoleReader consoleReader, StringBuffer data)
			throws IOException {
		while (true) {
			String line = consoleReader.readLine("... ");
			if (line == null || "".equals(line.trim())) {
				return;
			}
			data.append(line);
		}
	}

	private List<String> parseArgs(String line) {
		List<String> parts = new ArrayList<String>();
		String[] segments = StringUtils.delimitedListToStringArray(line, " ");
		StringBuffer part = new StringBuffer();
		boolean swallowWhitespace = false;
		for (String segment : segments) {
			if ("".equals(segment)) {
				continue;
			}
			if (segment.startsWith("\"")) {
				swallowWhitespace = true;
				part.append(segment.substring(1));
			}
			else if (segment.endsWith("\"")) {
				swallowWhitespace = false;
				part.append(" ").append(segment.substring(0, segment.length() - 1));
				parts.add(part.toString());
				part = new StringBuffer();
			}
			else {
				if (!swallowWhitespace) {
					parts.add(segment);
				}
				else {
					part.append(" ").append(segment);
				}
			}
		}
		if (part.length() > 0) {
			parts.add(part.toString());
		}
		return parts;
	}

	private void launchProcess(List<String> parts, final PrintStream sysout,
			final PrintStream syserr) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(parts);
			if (isJava7()) {
				inheritIO(processBuilder);
			}
			processBuilder.environment().putAll(System.getenv());
			Process process = processBuilder.start();
			if (!isJava7()) {
				ProcessGroovyMethods.consumeProcessOutput(process, (OutputStream) sysout,
						(OutputStream) syserr);
			}
			process.waitFor();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private boolean isJava7() {
		return PROCESS_BUILDER_INHERIT_IO_METHOD != null;
	}

	private void inheritIO(ProcessBuilder processBuilder) {
		ReflectionUtils.invokeMethod(PROCESS_BUILDER_INHERIT_IO_METHOD, processBuilder);
	}

	private void runCommand(List<String> args) {
		if (!getName().equals(args.get(0))) {
			this.springCli.runAndHandleErrors(args.toArray(new String[args.size()]));
		}
	}

	public void pushPrompt(String prompt) {
		this.prompts.push(this.prompt);
		this.prompt = prompt;
	}

	public String popPrompt() {
		if (this.prompts.isEmpty()) {
			this.prompt = this.defaultPrompt;
		}
		else {
			this.prompt = this.prompts.pop();
		}
		return this.prompt;
	}

	private static class ConsoleReaderOutputStream extends OutputStream {

		private ConsoleReader consoleReader;

		public ConsoleReaderOutputStream(ConsoleReader consoleReader) {
			this.consoleReader = consoleReader;
		}

		@Override
		public void write(int b) throws IOException {
			this.consoleReader.getOutput().write(b);
		}

	}
}
