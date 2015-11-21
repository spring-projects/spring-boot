/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.msgpack;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for MessagePack.
 *
 * @author Toshiaki Maki
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SampleMessagePackApplication.class)
@WebIntegrationTest(randomPort = true)
@DirtiesContext
public class SampleMessagePackApplicationTests {

	@Value("${local.server.port}")
	private int port;
	@Autowired
	RestTemplate restTemplate;

	ParameterizedTypeReference<Map<String, Object>> mapTypeRef = new ParameterizedTypeReference<Map<String, Object>>() {

	};

	@Test
	public void testHelloMessagePackAsBytes() throws Exception {
		RequestEntity req = RequestEntity.get(URI.create("http://localhost:" + this.port))
				.accept(MediaType.parseMediaType("application/x-msgpack")).build();
		ResponseEntity<byte[]> entity = restTemplate.exchange(req, byte[].class);
		assertThat(entity.getStatusCode(), is(HttpStatus.OK));
		assertThat(entity.getHeaders().getContentType().getType(), is("application"));
		assertThat(entity.getHeaders().getContentType().getSubtype(), is("x-msgpack"));
		byte[] body = { -126, -93, 102, 111, 111, -91, 104, 101, 108, 108, 111, -93, 98,
				97, 114, -91, 119, 111, 114, 108, 100 }; // less than JSON
		assertThat(entity.getBody(), is(body));
	}

	@Test
	public void testHelloJsonAsBytes() throws Exception {
		RequestEntity req = RequestEntity.get(URI.create("http://localhost:" + this.port))
				.accept(MediaType.parseMediaType("application/json")).build();
		ResponseEntity<byte[]> entity = restTemplate.exchange(req, byte[].class);
		assertThat(entity.getStatusCode(), is(HttpStatus.OK));
		assertThat(entity.getHeaders().getContentType().getType(), is("application"));
		assertThat(entity.getHeaders().getContentType().getSubtype(), is("json"));
		byte[] body = { 123, 34, 102, 111, 111, 34, 58, 34, 104, 101, 108, 108, 111, 34,
				44, 34, 98, 97, 114, 34, 58, 34, 119, 111, 114, 108, 100, 34, 125 };
		assertThat(entity.getBody(), is(body));
	}

	@Test
	public void testHelloMessagePackAsObject() throws Exception {
		RequestEntity req = RequestEntity.get(URI.create("http://localhost:" + this.port))
				.accept(MediaType.parseMediaType("application/x-msgpack")).build();
		ResponseEntity<Map<String, Object>> entity = restTemplate.exchange(req,
				mapTypeRef);
		assertThat(entity.getStatusCode(), is(HttpStatus.OK));
		assertThat(entity.getHeaders().getContentType().getType(), is("application"));
		assertThat(entity.getHeaders().getContentType().getSubtype(), is("x-msgpack"));
		Map<String, Object> obj = new LinkedHashMap<>();
		obj.put("foo", "hello");
		obj.put("bar", "world");
		assertThat(entity.getBody(), is(obj));
	}

	@Test
	public void testHelloJsonAsObject() throws Exception {
		RequestEntity req = RequestEntity.get(URI.create("http://localhost:" + this.port))
				.accept(MediaType.parseMediaType("application/json")).build();
		ResponseEntity<Map<String, Object>> entity = restTemplate.exchange(req,
				mapTypeRef);
		assertThat(entity.getStatusCode(), is(HttpStatus.OK));
		assertThat(entity.getHeaders().getContentType().getType(), is("application"));
		assertThat(entity.getHeaders().getContentType().getSubtype(), is("json"));
		Map<String, Object> obj = new LinkedHashMap<>();
		obj.put("foo", "hello");
		obj.put("bar", "world");
		assertThat(entity.getBody(), is(obj));
	}

	@Test
	public void testCalcMessagePackAsObject() throws Exception {
		RequestEntity req = RequestEntity
				.get(URI.create(
						"http://localhost:" + this.port + "/calc?left=100&right=200"))
				.accept(MediaType.parseMediaType("application/x-msgpack")).build();
		ResponseEntity<CalcResult> entity = restTemplate.exchange(req, CalcResult.class);
		assertThat(entity.getStatusCode(), is(HttpStatus.OK));
		assertThat(entity.getHeaders().getContentType().getType(), is("application"));
		assertThat(entity.getHeaders().getContentType().getSubtype(), is("x-msgpack"));
		assertThat(entity.getBody(), is(new CalcResult(100, 200, 300)));
	}

	@Test
	public void testCalcJsonObject() throws Exception {
		RequestEntity req = RequestEntity
				.get(URI.create(
						"http://localhost:" + this.port + "/calc?left=100&right=200"))
				.accept(MediaType.parseMediaType("application/json")).build();
		ResponseEntity<CalcResult> entity = restTemplate.exchange(req, CalcResult.class);
		assertThat(entity.getStatusCode(), is(HttpStatus.OK));
		assertThat(entity.getHeaders().getContentType().getType(), is("application"));
		assertThat(entity.getHeaders().getContentType().getSubtype(), is("json"));
		assertThat(entity.getBody(), is(new CalcResult(100, 200, 300)));
	}
}
