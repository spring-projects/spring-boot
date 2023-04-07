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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerHost}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerHostTests {

	private static final String MAC_HOST = "unix:///var/run/docker.sock";

	private static final String LINUX_HOST = "unix:///var/run/docker.sock";

	private static final String WINDOWS_HOST = "npipe:////./pipe/docker_engine";

	private static final String WSL_HOST = "unix:///var/run/docker.sock";

	private static final String HTTP_HOST = "http://192.168.1.1";

	private static final String HTTPS_HOST = "https://192.168.1.1";

	private static final String TCP_HOST = "tcp://192.168.1.1";

	private static final Function<String, String> NO_SYSTEM_ENV = (key) -> null;

	private static final Supplier<List<DockerCliContextResponse>> NO_CONTEXT = () -> Collections.emptyList();

	@Test
	void getWhenHasHost() {
		DockerHost host = DockerHost.get("192.168.1.1", NO_SYSTEM_ENV, NO_CONTEXT);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenHasServiceHostEnv() {
		Map<String, String> systemEnv = Map.of("SERVICES_HOST", "192.168.1.2");
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("192.168.1.2");
	}

	@Test
	void getWhenHasMacDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", MAC_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasLinuxDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", LINUX_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasWindowsDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", WINDOWS_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasWslDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", WSL_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasHttpDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", HTTP_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenHasHttpsDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", HTTPS_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenHasTcpDockerHostEnv() {
		Map<String, String> systemEnv = Map.of("DOCKER_HOST", TCP_HOST);
		DockerHost host = DockerHost.get(null, systemEnv::get, NO_CONTEXT);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenHasMacContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, MAC_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasLinuxContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, LINUX_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasWindowsContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, WINDOWS_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasWslContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, WSL_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("127.0.0.1");
	}

	@Test
	void getWhenHasHttpContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, HTTP_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenHasHttpsContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, HTTPS_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenHasTcpContext() {
		List<DockerCliContextResponse> context = List.of(new DockerCliContextResponse("test", true, TCP_HOST));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("192.168.1.1");
	}

	@Test
	void getWhenContextHasMultiple() {
		List<DockerCliContextResponse> context = new ArrayList<>();
		context.add(new DockerCliContextResponse("test", false, "http://192.168.1.1"));
		context.add(new DockerCliContextResponse("test", true, "http://192.168.1.2"));
		context.add(new DockerCliContextResponse("test", false, "http://192.168.1.3"));
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, () -> context);
		assertThat(host).hasToString("192.168.1.2");
	}

	@Test
	void getWhenHasNone() {
		DockerHost host = DockerHost.get(null, NO_SYSTEM_ENV, NO_CONTEXT);
		assertThat(host).hasToString("127.0.0.1");
	}

}
