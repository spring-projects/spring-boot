/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sni.server;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.restclient.autoconfigure.RestClientSsl;
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
