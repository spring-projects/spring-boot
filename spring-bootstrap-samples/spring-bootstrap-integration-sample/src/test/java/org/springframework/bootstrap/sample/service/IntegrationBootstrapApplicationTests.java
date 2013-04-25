package org.springframework.bootstrap.sample.service;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;

import static org.junit.Assert.assertEquals;

/**
 * Basic integration tests for service demo application.
 * 
 * @author Dave Syer
 * 
 */
public class IntegrationBootstrapApplicationTests {

	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return (ConfigurableApplicationContext) SpringApplication
										.run(IntegrationBootstrapApplication.class);
							}
						});
		context = future.get(10, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testVanillaExchange() throws Exception {
		MessagingTemplate template = new MessagingTemplate();
		template.setChannelResolver(new BeanFactoryChannelResolver(context));
		Message<?> result = template.sendAndReceive("input",
				MessageBuilder.withPayload("Phil").build());
		assertEquals("{message=Hello Phil}", result.getPayload().toString());
	}

}
