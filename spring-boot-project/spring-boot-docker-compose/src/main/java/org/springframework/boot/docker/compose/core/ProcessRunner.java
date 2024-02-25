/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;

/**
 * Runs a process and captures the result.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ProcessRunner {

	private static final String USR_LOCAL_BIN = "/usr/local/bin";

	private static final boolean MAC_OS = System.getProperty("os.name").toLowerCase().contains("mac");

	private static final Log logger = LogFactory.getLog(ProcessRunner.class);

	private final File workingDirectory;

	/**
	 * Create a new {@link ProcessRunner} instance.
	 */
	ProcessRunner() {
		this(null);
	}

	/**
	 * Create a new {@link ProcessRunner} instance.
	 * @param workingDirectory the working directory for the process
	 */
	ProcessRunner(File workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Runs the given {@code command}. If the process exits with an error code other than
	 * zero, an {@link ProcessExitException} will be thrown.
	 * @param command the command to run
	 * @return the output of the command
	 * @throws ProcessExitException if execution failed
	 */
	String run(String... command) {
		return run(null, command);
	}

	/**
	 * Runs the given {@code command}. If the process exits with an error code other than
	 * zero, an {@link ProcessExitException} will be thrown.
	 * @param outputConsumer consumer used to accept output one line at a time
	 * @param command the command to run
	 * @return the output of the command
	 * @throws ProcessExitException if execution failed
	 */
	String run(Consumer<String> outputConsumer, String... command) {
		logger.trace(LogMessage.of(() -> "Running '%s'".formatted(String.join(" ", command))));
		Process process = startProcess(command);
		ReaderThread stdOutReader = new ReaderThread(process.getInputStream(), "stdout", outputConsumer);
		ReaderThread stdErrReader = new ReaderThread(process.getErrorStream(), "stderr", outputConsumer);
		logger.trace("Waiting for process exit");
		int exitCode = waitForProcess(process);
		logger.trace(LogMessage.format("Process exited with exit code %d", exitCode));
		String stdOut = stdOutReader.toString();
		String stdErr = stdErrReader.toString();
		if (exitCode != 0) {
			throw new ProcessExitException(exitCode, command, stdOut, stdErr);
		}
		return stdOut;
	}

	/**
	 * Starts a new process with the given command.
	 * @param command the command to be executed
	 * @return the started process
	 * @throws ProcessStartException if an error occurs while starting the process
	 */
	private Process startProcess(String[] command) {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(this.workingDirectory);
		try {
			return processBuilder.start();
		}
		catch (IOException ex) {
			String path = processBuilder.environment().get("PATH");
			if (MAC_OS && path != null && !path.contains(USR_LOCAL_BIN)
					&& !command[0].startsWith(USR_LOCAL_BIN + "/")) {
				String[] localCommand = command.clone();
				localCommand[0] = USR_LOCAL_BIN + "/" + localCommand[0];
				return startProcess(localCommand);
			}
			throw new ProcessStartException(command, ex);
		}
	}

	/**
	 * Waits for the given process to complete and returns the exit value.
	 * @param process the process to wait for
	 * @return the exit value of the process
	 * @throws IllegalStateException if the waiting process is interrupted
	 */
	private int waitForProcess(Process process) {
		try {
			return process.waitFor();
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException("Interrupted waiting for %s".formatted(process));
		}
	}

	/**
	 * Thread used to read stream input from the process.
	 */
	private static class ReaderThread extends Thread {

		private final InputStream source;

		private final Consumer<String> outputConsumer;

		private final StringBuilder output = new StringBuilder();

		private final CountDownLatch latch = new CountDownLatch(1);

		/**
		 * Constructs a new ReaderThread object.
		 * @param source the InputStream source to read from
		 * @param name the name of the thread
		 * @param outputConsumer the consumer to handle the output
		 */
		ReaderThread(InputStream source, String name, Consumer<String> outputConsumer) {
			this.source = source;
			this.outputConsumer = outputConsumer;
			setName("OutputReader-" + name);
			setDaemon(true);
			start();
		}

		/**
		 * Runs the thread.
		 *
		 * This method reads the input stream from the source and appends each line to the
		 * output. If an output consumer is set, it also invokes the consumer with each
		 * line.
		 * @throws UncheckedIOException if there is an error reading the process stream
		 */
		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(this.source, StandardCharsets.UTF_8))) {
				String line = reader.readLine();
				while (line != null) {
					this.output.append(line);
					this.output.append("\n");
					if (this.outputConsumer != null) {
						this.outputConsumer.accept(line);
					}
					line = reader.readLine();
				}
				this.latch.countDown();
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to read process stream", ex);
			}
		}

		/**
		 * Returns a string representation of the object.
		 * @return the string representation of the object, or null if the thread is
		 * interrupted
		 */
		@Override
		public String toString() {
			try {
				this.latch.await();
				return this.output.toString();
			}
			catch (InterruptedException ex) {
				return null;
			}
		}

	}

}
