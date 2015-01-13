/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.core.DockerClientImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.ws.rs.ProcessingException;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.boot.autoconfigure.docker.DockerUtils.isConnected;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

public class DockerAutoConfigurationTest extends Assert {

	AnnotationConfigApplicationContext context;

	ClientAndServer dockerServer;

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
		context.register(DockerAutoConfiguration.class);

		int dockerPort = findAvailableTcpPort();
		dockerServer = new ClientAndServer(dockerPort);

		addEnvironment(context, "spring.docker.uri:http://localhost:" + dockerPort);
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}

		dockerServer.stop();
	}

	@Test
	public void shouldBeConnected() {
		context.refresh();

		dockerServer.when(request().withPath("/v1.15/_ping")).respond(response().withBody("OK"));

		DockerClient docker = context.getBean(DockerClient.class);
		assertTrue(isConnected(docker));
	}

	@Test
	public void shouldNotBeConnected() {
		context.refresh();

		dockerServer.when(request().withPath("/v1.15/_ping")).respond(response().withStatusCode(404));

		DockerClient docker = context.getBean(DockerClient.class);
		assertFalse(isConnected(docker));
	}

	@Test
	public void shouldUseGivenApiVersion() {
		addEnvironment(context, "spring.docker.version:1.14");
		context.refresh();

		dockerServer.when(request().withPath("/v1.14/_ping")).respond(response().withBody("OK"));

		DockerClient docker = context.getBean(DockerClient.class);
		assertTrue(isConnected(docker));
	}

	@Test
	public void shouldUseCredentials() {
		String username = "henry";
		String password = "secretPass";
		String email = "docker@gmail.com";
		addEnvironment(context,
				"spring.docker.username:" + username,
				"spring.docker.password:" + password,
				"spring.docker.email:" + email);
		context.refresh();

		DockerClientImpl docker = (DockerClientImpl) context.getBean(DockerClient.class);
		assertEquals(username, docker.authConfig().getUsername());
		assertEquals(password, docker.authConfig().getPassword());
		assertEquals(email, docker.authConfig().getEmail());
	}

}

final class DockerUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DockerUtils.class);

	private DockerUtils() {
	}

	public static boolean isConnected(DockerClient docker) {
		try {
			docker.pingCmd().exec();
			return true;
		} catch (ProcessingException e) {
			LOG.debug("Can't connect to the Docker server.", e);
			return false;
		} catch (DockerException e) {
			LOG.debug("Can't connect to the Docker server.", e);
			return false;
		}
	}

}