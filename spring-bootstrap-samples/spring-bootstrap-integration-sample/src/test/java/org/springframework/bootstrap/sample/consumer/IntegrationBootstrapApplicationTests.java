package org.springframework.bootstrap.sample.consumer;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.sample.consumer.IntegrationBootstrapApplication;
import org.springframework.bootstrap.sample.producer.ProducerApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertTrue;

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
		context = (ConfigurableApplicationContext) SpringApplication
				.run(IntegrationBootstrapApplication.class);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testVanillaExchange() throws Exception {
		SpringApplication.run(ProducerApplication.class, "World");
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	private String getOutput() throws Exception {
		Future<String> future = Executors.newSingleThreadExecutor().submit(
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						Resource[] resources = new Resource[0];
						while (resources.length == 0) {
							Thread.sleep(200);
							resources = ResourcePatternUtils.getResourcePatternResolver(
									new DefaultResourceLoader()).getResources(
									"file:target/output/**");
						}
						StringBuilder builder = new StringBuilder();
						for (Resource resource : resources) {
							builder.append(new String(StreamUtils
									.copyToByteArray(resource.getInputStream())));
						}
						return builder.toString();
					}
				});
		return future.get(10, TimeUnit.SECONDS);
	}
}
