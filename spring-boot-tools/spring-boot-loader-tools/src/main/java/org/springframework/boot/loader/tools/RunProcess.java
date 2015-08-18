/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.util.ReflectionUtils;

/**
 * Utility used to run a process.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.1.0
 */
public class RunProcess {

	private static final Method INHERIT_IO_METHOD = ReflectionUtils.findMethod(
			ProcessBuilder.class, "inheritIO");

	private static final long JUST_ENDED_LIMIT = 500;

	private final String[] command;

	private volatile Process process;

	private volatile long endTime;

	public RunProcess(String... command) {
		this.command = command;
	}

	public int run(boolean waitForProcess, String... args) throws IOException {
		return run(waitForProcess, Arrays.asList(args));
	}

	protected int run(boolean waitForProcess, Collection<String> args) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(this.command);
		builder.command().addAll(args);
		builder.redirectErrorStream(true);
		boolean inheritedIO = inheritIO(builder);
		try {
			Process process = builder.start();
			this.process = process;
			if (!inheritedIO) {
				redirectOutput(process);
			}
			SignalUtils.attachSignalHandler(new Runnable() {
				@Override
				public void run() {
					handleSigInt();
				}
			});
			if (waitForProcess) {
				try {
					return process.waitFor();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					return 1;
				}
			}
			return 5;
		}
		finally {
			if (waitForProcess) {
				this.endTime = System.currentTimeMillis();
				this.process = null;
			}
		}
	}

	private boolean inheritIO(ProcessBuilder builder) {
		if (isInheritIOBroken()) {
			return false;
		}
		try {
			INHERIT_IO_METHOD.invoke(builder);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	// There's a bug in the Windows VM (https://bugs.openjdk.java.net/browse/JDK-8023130)
	// that means we need to avoid inheritIO
	private static boolean isInheritIOBroken() {
		if (!System.getProperty("os.name", "none").toLowerCase().contains("windows")) {
			return false;
		}
		String runtime = System.getProperty("java.runtime.version");
		if (!runtime.startsWith("1.7")) {
			return false;
		}
		String[] tokens = runtime.split("_");
		if (tokens.length < 2) {
			return true; // No idea actually, shouldn't happen
		}
		try {
			Integer build = Integer.valueOf(tokens[1].split("[^0-9]")[0]);
			if (build < 60) {
				return true;
			}
		}
		catch (Exception ex) {
			return true;
		}
		return false;
	}

	private void redirectOutput(Process process) {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		new Thread() {

			@Override
			public void run() {
				try {
					String line = reader.readLine();
					while (line != null) {
						System.out.println(line);
						line = reader.readLine();
						System.out.flush();
					}
					reader.close();
				}
				catch (Exception ex) {
				}
			}

		}.start();
	}

	/**
	 * @return the running process or {@code null}
	 */
	public Process getRunningProcess() {
		return this.process;
	}

	/**
	 * @return {@code true} if the process was stopped.
	 */
	public boolean handleSigInt() {

		// if the process has just ended, probably due to this SIGINT, consider handled.
		if (hasJustEnded()) {
			return true;
		}
		return doKill();

	}

	/**
	 * Kill this process.
	 */
	public void kill() {
		doKill();
	}

	private boolean doKill() {
		// destroy the running process
		Process process = this.process;
		if (process != null) {
			try {
				process.destroy();
				process.waitFor();
				this.process = null;
				return true;
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		return false;
	}

	public boolean hasJustEnded() {
		return System.currentTimeMillis() < (this.endTime + JUST_ENDED_LIMIT);
	}

}
