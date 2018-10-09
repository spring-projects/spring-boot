package org.springframework.boot.actuate.trace.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.boot.actuate.trace.http.HttpTrace.Principal;
import org.springframework.boot.actuate.trace.http.HttpTrace.Request;
import org.springframework.boot.actuate.trace.http.HttpTrace.Response;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class HttpTraceTest {

	private final ObjectMapper objectMapper;
	private final String exampleJson;

	public HttpTraceTest() throws IOException {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		ClassPathResource exampleJsonResource = new ClassPathResource("trace/http/example-trace.json");
		try(InputStream is = exampleJsonResource.getInputStream()) {
			exampleJson = IOUtils.toString(is).trim();
		}
	}

	@Test
	public void jsonSerialization() throws IOException, URISyntaxException {
		HttpTrace trace = createHttpTrace();

		String jsonValue = objectMapper.writeValueAsString(trace);

		assertEquals(exampleJson, jsonValue);
	}

	@Test
	public void jsonDeserialization() throws IOException, URISyntaxException {
		HttpTrace deserializedTrace = objectMapper.readValue(exampleJson, HttpTrace.class);
		HttpTrace originalTrace = createHttpTrace();

		assertEquals(originalTrace.getTimestamp(), deserializedTrace.getTimestamp());
		assertEquals(originalTrace.getTimeTaken(), deserializedTrace.getTimeTaken());
		assertEquals(originalTrace.getPrincipal().getName(), deserializedTrace.getPrincipal().getName());
		assertEquals(originalTrace.getSession().getId(), deserializedTrace.getSession().getId());

		assertEquals(originalTrace.getRequest().getMethod(), deserializedTrace.getRequest().getMethod());
		assertEquals(originalTrace.getRequest().getUri(), deserializedTrace.getRequest().getUri());
		assertEquals(originalTrace.getRequest().getRemoteAddress(), deserializedTrace.getRequest().getRemoteAddress());
		assertEquals(originalTrace.getRequest().getHeaders(), deserializedTrace.getRequest().getHeaders());

		assertEquals(originalTrace.getResponse().getStatus(), deserializedTrace.getResponse().getStatus());
		assertEquals(originalTrace.getResponse().getHeaders(), deserializedTrace.getResponse().getHeaders());


	}

	@NotNull
	private static HttpTrace createHttpTrace() throws URISyntaxException {
		HttpTrace trace = new HttpTrace();
		Map<String, List<String>> requestHeader = singletonMap("X-Req-Header", singletonList("reqHeaderValue"));
		Map<String, List<String>> responseHeader = singletonMap("X-Resp-Header", singletonList("respHeaderValue"));
		Instant timestamp = Instant.parse("2018-10-09T04:16:26.979Z");

		trace.setTimestamp(timestamp);
		trace.setPrincipal(new Principal("principal"));
		trace.setResponse(new Response(200, requestHeader));
		trace.setRequest(new Request("GET", new URI("http://example.com"), responseHeader, "192.168.0.1"));
		trace.setSessionId("sessionId");
		trace.setTimeTaken(100L);
		return trace;
	}

}
