package sample.data.couchbase;

import java.net.ConnectException;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.OutputCapture;
import org.springframework.core.NestedCheckedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleCouchbaseApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() throws Exception {
		try {
			new SpringApplicationBuilder(SampleCouchbaseApplication.class)
					.run("--server.port=0");
		}
		catch (RuntimeException ex) {
			if (serverNotRunning(ex)) {
				return;
			}
		}
		String output = this.outputCapture.toString();
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

	private boolean serverNotRunning(RuntimeException ex) {
		@SuppressWarnings("serial")
		NestedCheckedException nested = new NestedCheckedException("failed", ex) {
		};
		if (nested.contains(ConnectException.class)) {
			Throwable root = nested.getRootCause();
			if (root.getMessage().contains("Connection refused")) {
				return true;
			}
		}
		return false;
	}

}
