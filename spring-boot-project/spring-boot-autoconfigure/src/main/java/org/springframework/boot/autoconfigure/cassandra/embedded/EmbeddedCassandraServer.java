/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra.embedded;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A Simple implementation of embedded cassandra server. The server will be run under a new Java process.
 *
 * @author Dmytro Nosan
 */
public final class EmbeddedCassandraServer {


	private static final Log logger = LogFactory.getLog(EmbeddedCassandraServer.class);

	private final CassandraProcess process = new CassandraProcess();
	private Path workingDir;
	private Config config;
	private Duration startupTimeout;
	private List<String> args = new ArrayList<>();

	/**
	 * Retrieve the server additional arguments.
	 *
	 * @return additional arguments.
	 */
	public List<String> getArgs() {
		return this.args;
	}

	/**
	 * Additional arguments which should be associated with cassandra process.
	 *
	 * @param args array of additional arguments.
	 */
	public void setArgs(List<String> args) {
		this.args = args;
	}

	/**
	 * Retrieve the server startup timeout.
	 *
	 * @return startup timeout
	 */
	public Duration getStartupTimeout() {
		return this.startupTimeout;
	}

	/**
	 * Set up startup timeout. Set '0' to run the server without awaiting.
	 *
	 * @param startupTimeout startup timeout
	 */
	public void setStartupTimeout(Duration startupTimeout) {
		this.startupTimeout = startupTimeout;
	}

	/**
	 * Set up working directory.
	 *
	 * @param workingDir working directory.
	 */
	public void setWorkingDir(Path workingDir) {
		this.workingDir = workingDir;
	}

	/**
	 * Retrieve the server working directory.
	 *
	 * @return working directory.
	 */
	public Path getWorkingDir() {
		return this.workingDir;
	}


	/**
	 * Set up the cassandra config.
	 *
	 * @param config Configuration
	 */
	public void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * Retrieve the server configuration.
	 *
	 * @return configuration
	 */
	public Config getConfig() {
		return this.config;
	}


	/**
	 * Start the server and wait until it will be ready to accept new connections.
	 * Note! awaiting the server will be worked only if startup timeout was set.
	 * The server will be run under a new Java process.
	 *
	 * @throws IOException Process can not be started.
	 */
	public void start() throws IOException {
		Assert.notNull(this.config, "Cassandra config must not be null");

		if (this.workingDir == null) {
			this.workingDir = Files.createTempDirectory("cassandra");
		}
		else if (!Files.exists(this.workingDir)) {
			Files.createDirectory(this.workingDir);
		}


		Path configurationFile = this.workingDir.resolve("cassandra.yaml");

		try (FileWriter writer = new FileWriter(configurationFile.toFile())) {
			YamlUtils.dump(this.config, writer);
		}

		List<String> runArgs = new ArrayList<>();
		runArgs.add("-Dcassandra.config=" + configurationFile.toUri());
		runArgs.add("-Dcassandra.storagedir=" + this.workingDir.toAbsolutePath());
		runArgs.addAll((this.args == null ? Collections.emptyList() : this.args));

		logger.info("Starting the new cassandra server using directory '"
				+ this.workingDir + "' with arguments " + runArgs);

		this.process.start(this.workingDir, runArgs);

		if (this.startupTimeout != null && this.startupTimeout.toMillis() > 0) {
			await(this.startupTimeout);
		}

	}

	/**
	 * Stop the server.
	 */
	public void stop() {
		if (isRunning()) {
			logger.info("Stopping the cassandra server...");
			this.process.destroy();
		}
	}


	/**
	 * Check whether the server is running and ready to accept connections or not.
	 *
	 * @return true if the server is running.
	 */
	public boolean isRunning() {
		return this.process.isAlive() && tryConnect();
	}

	private void await(Duration timeout) {
		long startTime = System.nanoTime();
		long rem = timeout.toNanos();
		do {
			if (!this.process.isAlive()) {
				throw new IllegalStateException("Cassandra is not started.");
			}

			if (tryConnect()) {
				logger.info("An embedded cassandra server '" + getHost() + ":" + getPort()
						+ "' is ready to accept new connections.");
				return;
			}

			if (rem > 0) {
				try {
					Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 500));
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			rem = timeout.toNanos() - (System.nanoTime() - startTime);
		} while (rem > 0);

		stop();

		throw new IllegalStateException("Cassandra is not started after '" + timeout.toMillis()
				+ "' ms. Please increase startup timeout.");
	}


	private boolean tryConnect() {
		try (Socket ignored = new Socket(getHost(), getPort())) {
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	private String getHost() {
		Config config = this.config;
		if (config.start_native_transport) {
			return Objects.toString(config.listen_address, "localhost");
		}
		else if (config.start_rpc) {
			return Objects.toString(config.rpc_address, "localhost");
		}
		return "localhost";
	}


	private int getPort() {
		Config config = this.config;
		if (config.start_native_transport) {
			return config.native_transport_port;
		}
		else if (config.start_rpc) {
			return config.rpc_port;
		}
		return 0;
	}


	/**
	 * Utility class for running cassandra in the forked jvm process.
	 */
	private static class CassandraProcess {


		private Process process;


		private synchronized void start(Path workingDirectory, List<String> args) throws IOException {
			Assert.state(this.process == null, "Cassandra process is already initialized.");
			List<String> arguments = new ArrayList<>();
			arguments.add(getJava());
			arguments.addAll(getClasspath());
			arguments.addAll(args);
			arguments.add(CassandraDaemon.class.getCanonicalName());

			ProcessBuilder processBuilder = new ProcessBuilder(arguments);
			processBuilder.directory(workingDirectory.toFile());
			processBuilder.redirectErrorStream(true);
			this.process = processBuilder.start();
			Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
			new Thread(new Redirection(this.process.getInputStream(), System.out)).start();

		}

		private synchronized void destroy() {
			if (this.process != null) {
				this.process.destroy();
				try {
					if (!this.process.waitFor(5, TimeUnit.SECONDS)) {
						this.process.destroyForcibly();
					}
					if (!this.process.waitFor(30, TimeUnit.SECONDS)) {
						this.process.destroyForcibly();
					}
					if (!this.process.waitFor(1, TimeUnit.MINUTES)) {
						throw new IllegalStateException("Cassandra process has not been destroyed.");
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					this.process = null;
				}
			}
		}

		private synchronized boolean isAlive() {
			return this.process != null && this.process.isAlive();
		}

		private String getJava() throws IOException {

			String javaHome = System.getProperty("java.home");
			Assert.hasText(javaHome, "Unable to find 'java home'");

			File parent = new File(javaHome);

			File javaExecutable = new File(parent, "bin/java");

			if (!javaExecutable.exists()) {
				javaExecutable = new File(parent, "bin/java.exe");
			}

			Assert.isTrue(javaExecutable.exists(), "Unable to find '/bin/java.exe' or '/bin/java' in " + javaHome);

			return javaExecutable.getCanonicalPath();
		}


		private List<String> getClasspath() {
			String classpath = System.getProperty("java.class.path");
			Assert.hasText(classpath, "Unable to find 'java.class.path'");
			List<String> args = new ArrayList<>();
			args.add("-cp");
			args.add(classpath);
			return args;
		}


		private static final class Redirection implements Runnable {

			private final InputStream from;
			private final PrintStream to;

			private Redirection(InputStream stream, PrintStream to) {
				this.from = stream;
				this.to = to;
			}

			@Override
			public void run() {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.from))) {
					String line;
					while ((line = reader.readLine()) != null) {
						this.to.println(line);
					}
				}
				catch (IOException ex) {
					logger.error("IO exception during reading output from an embedded cassandra server.", ex);
				}
			}

		}
	}

	/**
	 * Utility class for serializing {@link Config config}.
	 */
	private abstract static class YamlUtils {


		static void dump(Config config, Writer writer) {
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(new ConfigRepresenter(), options);
			yaml.dump(config, writer);
		}

		/**
		 * Utility class for serializing {@link ParameterizedClass}.
		 * Represents a block:
		 * seed_provider:
		 * - class_name: org.apache.cassandra.locator.SimpleSeedProvider
		 * parameters:
		 * - seeds: "localhost"
		 */
		private static final class ConfigRepresenter extends Representer {

			private ConfigRepresenter() {
				this.representers.put(ParameterizedClass.class, new RepresentParameterizedClass());
				addClassTag(Config.class, Tag.MAP);
			}

			@Override
			protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,
					Tag customTag) {
				if (propertyValue == null) {
					return null;
				}
				return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
			}

			private final class RepresentParameterizedClass implements Represent {

				@Override
				public Node representData(Object data) {

					ParameterizedClass parameterizedClass = (ParameterizedClass) data;

					LinkedHashMap<String, Object> source = new LinkedHashMap<>();

					if (!CollectionUtils.isEmpty(parameterizedClass.parameters)) {
						List<Map<String, String>> parameters = parameterizedClass.parameters.entrySet()
								.stream()
								.map(entry -> Collections.singletonMap(entry.getKey(), entry.getValue()))
								.collect(Collectors.toList());
						source.put(ParameterizedClass.PARAMETERS, parameters);
					}

					if (StringUtils.hasText(parameterizedClass.class_name)) {
						source.put(ParameterizedClass.CLASS_NAME, parameterizedClass.class_name);
					}

					return representSequence(Tag.SEQ, Collections.singletonList(source), false);
				}
			}
		}

	}
}
