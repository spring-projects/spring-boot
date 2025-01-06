/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.sni.server;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.boot.ssl.SslBundles;

@SpringBootApplication
public class SniClientApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(SniClientApplication.class)
				.web(WebApplicationType.NONE).run(args);
	}

	@Bean
	public RestClient restClient(RestClient.Builder restClientBuilder, RestClientSsl ssl) {
		return restClientBuilder.apply(ssl.fromBundle("server")).build();
	}

	@Bean
	public CommandLineRunner commandLineRunner(RestClient client) {
		return ((args) -> {
			for (String hostname : args) {
				callServer(client, hostname);
				callActuator(client, hostname);
			}
		});
	}

	private static void callServer(RestClient client, String hostname) {
		String url = "https://" + hostname + ":8443/";
		System.out.println(">>>>>> Calling server at '" + url + "'");
		try {
			ResponseEntity<String> response = client.get().uri(url).retrieve().toEntity(String.class);
			System.out.println(">>>>>> Server response status code is '" + response.getStatusCode() + "'");
			System.out.println(">>>>>> Server response body is '" + response + "'");
		} catch (Exception ex) {
			System.out.println(">>>>>> Exception thrown calling server at '" + url + "': " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private static void callActuator(RestClient client, String hostname) {
		String url = "https://" + hostname + ":8444/actuator/health";
		System.out.println(">>>>>> Calling server actuator at '" + url + "'");
		try {
			ResponseEntity<String> response = client.get().uri(url).retrieve().toEntity(String.class);
			System.out.println(">>>>>> Server actuator response status code is '" + response.getStatusCode() + "'");
			System.out.println(">>>>>> Server actuator response body is '" + response + "'");
		} catch (Exception ex) {
			System.out.println(">>>>>> Exception thrown calling server actuator at '" + url + "': " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
