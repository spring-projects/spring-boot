/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.session.redis;

import java.net.URI;

import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ServerPortInfoApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionRedisApplication}.
 *
 * @author Andy Wilkinson
 */
public class SampleSessionRedisApplicationTests {

	@Test
	public void sessionExpiry() throws Exception {

		String port = null;

		try {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.sources(SampleSessionRedisApplication.class)
					.properties("server.port:0")
					.initializers(new ServerPortInfoApplicationContextInitializer())
					.run();
			port = context.getEnvironment().getProperty("local.server.port");
		}
		catch (RuntimeException ex) {
			if (!redisServerRunning(ex)) {
				return;
			}
			throw ex;
		}

		URI uri = URI.create("http://localhost:" + port + "/");
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		String uuid1 = response.getBody();
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Cookie", response.getHeaders().getFirst("Set-Cookie"));

		RequestEntity<Void> request = new RequestEntity<Void>(requestHeaders,
				HttpMethod.GET, uri);

		String uuid2 = restTemplate.exchange(request, String.class).getBody();
		assertThat(uuid1).isEqualTo(uuid2);

		Thread.sleep(5000);

		String uuid3 = restTemplate.exchange(request, String.class).getBody();
		assertThat(uuid2).isNotEqualTo(uuid3);
	}

	private boolean redisServerRunning(Throwable ex) {
		if (ex instanceof RedisConnectionFailureException) {
			return false;
		}
		return (ex.getCause() == null || redisServerRunning(ex.getCause()));
	}

}
