package org.springframework.boot.cli.compiler;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link RepositoryConfigurationFactory}.
 * @author Johannes Stelzer
 */
public class RepositoryConfigurationFactoryTest {

	@Test
	public void createRepositoryConfiguration() {

		List<RepositoryConfiguration> repoConfig = RepositoryConfigurationFactory
				.createRepositoryConfiguration(Arrays.asList(
						"default::http://repo1.maven.org/maven2/",
						"custom::http://repo.intern/maven2/"));
		assertEquals(2, repoConfig.size());

		assertEquals("default", repoConfig.get(0).getName());
		assertEquals(URI.create("http://repo1.maven.org/maven2/"), repoConfig.get(0)
				.getUri());

		assertEquals("custom", repoConfig.get(1).getName());
		assertEquals(URI.create("http://repo.intern/maven2/"), repoConfig.get(1).getUri());
	}

	@Test(expected = IllegalArgumentException.class)
	public void createRepositoryConfiguration_failure() {
		RepositoryConfigurationFactory.createRepositoryConfiguration(Arrays
				.asList("default=http://repo1.maven.org/maven2/"));
	}
}