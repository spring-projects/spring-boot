package org.springframework.boot.actuate.autoconfigure;

import java.io.FileReader;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ApplicationPidFileWriterConfiguration}.
 *
 * @author Laurent Bardiaux
 */
public class ApplicationPidFileWriterConfigurationTest {

	private ConfigurableApplicationContext context;
	private SpringApplication application;

	@Before
	public void setup() {
		this.application = new SpringApplication(
				ApplicationPidFileWriterConfiguration.class);
		this.application.setWebEnvironment(false);

	}

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testWithNoProperty() {
		this.context = this.application.run();
		assertThat(this.context.containsBean("applicationPidFileWriter"), equalTo(false));
	}

	@Test
	public void testWithPropertyFalse() {
		this.context = this.application.run("--spring.pidfile.enabled=false");
		assertThat(this.context.containsBean("applicationPidFileWriter"), equalTo(false));
	}

	@Test
	public void testWithPropertyTrue() throws IOException {
		String pidFileName = this.temporaryFolder.newFile().getAbsolutePath();
		this.context = this.application.run("--spring.pidfile.enabled=true",
				"--spring.pidfile=" + pidFileName);

		assertThat(this.context.containsBean("applicationPidFileWriter"), equalTo(true));
		assertThat(FileCopyUtils.copyToString(new FileReader(pidFileName)),
				not(isEmptyString()));

	}

}
