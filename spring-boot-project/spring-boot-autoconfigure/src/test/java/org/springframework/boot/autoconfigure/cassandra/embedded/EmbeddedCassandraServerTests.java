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

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.locator.SimpleSeedProvider;
import org.apache.cassandra.locator.SimpleSnitch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedCassandraServer}.
 *
 * @author Dmytro Nosan
 */
public class EmbeddedCassandraServerTests {


	@Rule
	public OutputCapture outputCapture = new OutputCapture();
	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void invalidConfig() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cassandra is not started.");

		EmbeddedCassandraServer cassandra = start(new Config(), null, Duration.ofSeconds(5));

		assertThat(this.outputCapture.toString())
				.contains("Exception encountered during startup");

		assertThat(cassandra.isRunning())
				.withFailMessage("Cassandra is running. Cassandra should not be started if something wrong with " +
						"config.")
				.isFalse();
	}

	@Test
	public void timeout() throws Exception {

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cassandra is not started after '1' ms. Please increase startup timeout.");

		EmbeddedCassandraServer cassandra = start(config(), null, Duration.ofMillis(1));

		assertThat(cassandra.isRunning())
				.withFailMessage("Cassandra is running. Timeout method must 'stop' it.")
				.isFalse();

	}

	@Test
	public void withoutAwait() throws Exception {

		EmbeddedCassandraServer cassandra = start(config(), null, null);

		assertThat(cassandra.isRunning())
				.withFailMessage("Cassandra is running. Cassandra can not be started so fast.")
				.isFalse();

		assertThat(await(Duration.ofMinutes(2), cassandra::isRunning))
				.withFailMessage("Cassandra is not running.")
				.isTrue();

	}

	@Test
	public void alreadyStarted() throws Exception {

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cassandra process is already initialized");

		EmbeddedCassandraServer cassandra = start(config(), null, null);
		cassandra.start();

	}


	@Test
	public void args() throws Exception {

		ServerSocket ss = new ServerSocket(0);
		int port = ss.getLocalPort();
		Config config = config();

		ss.close();

		start(config, null, null, "-agentlib:jdwp=transport=dt_socket,address=" + port + ",server=y,suspend=y");

		assertThat(await(Duration.ofSeconds(5), () -> this.outputCapture.toString()
				.contains("Listening for transport dt_socket at address: " + port)))
				.withFailMessage("Debug mode is not running")
				.isTrue();


	}

	@Test
	public void customDirectory() throws Exception {

		File workingDir = new File("target/cassandra");
		FileSystemUtils.deleteRecursively(workingDir);

		Config config = config();

		start(config, workingDir.toPath(), Duration.ofMinutes(1));

		keyspace("boot", config);
		table("boot", "user", config);

		assertThat(workingDir).isDirectory();
		assertThat(workingDir.listFiles()).isNotEmpty();


	}


	@Test
	public void startStop() throws Exception {

		Config config = config();

		EmbeddedCassandraServer cassandra = start(config, null, Duration.ofMinutes(1));

		assertThat(cassandra.isRunning())
				.withFailMessage("Cassandra is not running")
				.isTrue();
		keyspace("boot", config);
		table("boot", "user", config);

		cassandra.stop();

		cassandra.start();

		assertThat(cassandra.isRunning())
				.withFailMessage("Cassandra is not running")
				.isTrue();
		keyspace("boot", config);
		table("boot", "user", config);

		cassandra.stop();

	}

	private static EmbeddedCassandraServer start(Config config, Path workingDirectory, Duration timeout, String... args)
			throws Exception {
		EmbeddedCassandraServer cassandra = new EmbeddedCassandraServer();
		cassandra.setConfig(config);
		cassandra.setStartupTimeout(timeout);
		cassandra.setWorkingDir(workingDirectory);
		cassandra.setArgs(Arrays.asList(args));
		cassandra.start();
		return cassandra;
	}


	private static Config config() {

		Config config = new Config();

		Iterator<Integer> ports = SocketUtils.findAvailableTcpPorts(4).iterator();

		config.native_transport_port = ports.next();
		config.rpc_port = ports.next();
		config.storage_port = ports.next();
		config.ssl_storage_port = ports.next();
		config.start_native_transport = true;
		config.rpc_address = "localhost";
		config.listen_address = "localhost";
		config.commitlog_sync = Config.CommitLogSync.periodic;
		config.commitlog_sync_period_in_ms = 10_000;
		config.partitioner = Murmur3Partitioner.class.getCanonicalName();
		config.endpoint_snitch = SimpleSnitch.class.getCanonicalName();
		config.seed_provider = new ParameterizedClass(SimpleSeedProvider.class.getCanonicalName(),
				Collections.singletonMap("seeds", "localhost"));

		return config;
	}


	private static boolean await(Duration timeout, Callable<Boolean> callable) throws Exception {

		long startTime = System.nanoTime();
		long rem = timeout.toNanos();
		do {
			Boolean result = callable.call();
			if (result) {
				return true;
			}
			if (rem > 0) {
				Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 500));
			}
			rem = timeout.toNanos() - (System.nanoTime() - startTime);
		} while (rem > 0);

		return false;
	}


	private static void keyspace(String keyspace, Config config) {
		try (Cluster cluster = cluster(config)) {
			Session session = cluster.connect();
			session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace
					+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
		}
	}

	private static void table(String keyspace, String table, Config config) {
		try (Cluster cluster = cluster(config)) {
			Session session = cluster.connect();
			session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + "." + table + " ( " +
					"  id text PRIMARY KEY )");
		}
	}

	private static Cluster cluster(Config config) {
		return Cluster.builder()
				.addContactPoint(config.listen_address)
				.withPort(config.native_transport_port)
				.build();
	}
}
