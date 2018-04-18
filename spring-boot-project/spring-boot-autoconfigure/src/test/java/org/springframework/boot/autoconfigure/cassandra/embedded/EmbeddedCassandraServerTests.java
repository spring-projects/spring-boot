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
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;

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
	public void shouldFailInvalidConfig() throws IOException {

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cassandra is not started.");

		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(new Config());
		server.setStartupTimeout(Duration.ofSeconds(15));
		server.start();

		assertThat(this.outputCapture.toString())
				.contains("Exception encountered during startup");
		assertThat(server.isRunning()).isFalse();


	}

	@Test
	public void timeout() throws IOException {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cassandra is not started after '1' ms. Please increase startup timeout.");

		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(new Config());
		server.setStartupTimeout(Duration.ofMillis(1));
		server.start();
		assertThat(server.isRunning()).isFalse();



	}


	@Test
	public void shouldRunWithoutAwait() throws IOException {
		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(getConfig());
		server.start();

		assertThat(server.isRunning()).isFalse();
	}

	@Test
	public void doubleStartFail() throws IOException {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cassandra process is already initialized");
		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(getConfig());
		server.start();
		server.start();
	}

	@Test
	public void shouldEnableDebugMode() throws IOException, InterruptedException {
		ServerSocket serverSocket = new ServerSocket(0);
		int p = serverSocket.getLocalPort();
		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(getConfig());
		server.setArgs(Collections.singletonList("-agentlib:jdwp=transport=dt_socket," +
				"address=" + p + ",server=y,suspend=y"));
		serverSocket.close();
		server.start();
		Thread.sleep(5000);
		assertThat(this.outputCapture.toString())
				.contains("Listening for transport dt_socket at address: " + p);
		assertThat(server.isRunning()).isEqualTo(false);

	}

	@Test
	public void shouldUseCustomDir() throws IOException {
		File workingDir = new File("target/cassandra");
		FileSystemUtils.deleteRecursively(workingDir);

		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(getConfig());
		server.setStartupTimeout(Duration.ofMinutes(1));
		server.setWorkingDir(workingDir.toPath());

		server.start();
		assertThat(server.isRunning()).isTrue();

		createTable(server, "users");
		server.stop();

		assertThat(server.isRunning()).isFalse();
		assertThat(workingDir).isDirectory();
		assertThat(workingDir.listFiles()).isNotEmpty();

	}

	@Test
	public void startStopLifecycle() throws IOException {

		EmbeddedCassandraServer server = new EmbeddedCassandraServer();
		server.setConfig(getConfig());
		server.setStartupTimeout(Duration.ofMinutes(1));

		server.start();
		assertThat(server.isRunning()).isTrue();
		createTable(server, "users");
		server.stop();
		assertThat(server.isRunning()).isFalse();

		server.start();
		assertThat(server.isRunning()).isTrue();
		createTable(server, "UsersUsers");
		server.stop();
		assertThat(server.isRunning()).isFalse();


	}

	private void createTable(EmbeddedCassandraServer server, String table) {
		try (Cluster cluster = Cluster.builder().addContactPoint("localhost")
				.withPort(server.getConfig().native_transport_port)
				.build()) {
			Session session = cluster.connect();
			session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
					+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");

			session.execute("CREATE TABLE IF NOT EXISTS boot_test." + table + " ( " +
					"  id text PRIMARY KEY, " +
					"  first_name text," +
					");");
		}

	}


	private Config getConfig() {

		Iterator<Integer> iterator = SocketUtils.findAvailableTcpPorts(4).iterator();
		Config config = new Config();
		config.native_transport_port = iterator.next();
		config.rpc_port = iterator.next();
		config.storage_port = iterator.next();
		config.ssl_storage_port = iterator.next();
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
}
