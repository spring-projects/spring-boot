package org.springframework.boot.actuate.system;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Created by jkubrynski@gmail.com / 2014-03-25
 */
public class ApplicationPidListenerTest {

	private static final String[] NO_ARGS = {};

	public static final String PID_FILE_NAME = "test.pid";

	@Test
	public void shouldCreatePidFile() {
		//given
		ApplicationPidListener sut = new ApplicationPidListener();
		sut.setPidFileName(PID_FILE_NAME);

		//when
		sut.onApplicationEvent(new ApplicationStartedEvent(
				new SpringApplication(), NO_ARGS));

		//then
		File pidFile = new File(PID_FILE_NAME);
		assertTrue(pidFile.exists());
		pidFile.delete();
	}

}
