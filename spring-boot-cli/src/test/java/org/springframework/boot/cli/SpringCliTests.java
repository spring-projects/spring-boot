package org.springframework.boot.cli;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.NoSuchCommandException;
import org.springframework.boot.cli.SpringCli;
import org.springframework.boot.cli.SpringCli.NoArgumentsException;
import org.springframework.boot.cli.SpringCli.NoHelpCommandArgumentsException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringCli}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class SpringCliTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SpringCli cli;

	@Mock
	private Command regularCommand;

	private Set<Call> calls = EnumSet.noneOf(Call.class);

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.cli = new SpringCli() {

			@Override
			protected void showUsage() {
				SpringCliTests.this.calls.add(Call.SHOW_USAGE);
				super.showUsage();
			};

			@Override
			protected void errorMessage(String message) {
				SpringCliTests.this.calls.add(Call.ERROR_MESSAGE);
				super.errorMessage(message);
			}

			@Override
			protected void printStackTrace(Exception ex) {
				SpringCliTests.this.calls.add(Call.PRINT_STACK_TRACE);
				super.printStackTrace(ex);
			}
		};
		given(this.regularCommand.getName()).willReturn("command");
		given(this.regularCommand.getDescription()).willReturn("A regular command");
		this.cli.setCommands(Arrays.asList(this.regularCommand));
	}

	@Test
	public void runWithoutArguments() throws Exception {
		this.thrown.expect(NoArgumentsException.class);
		this.cli.run();
	}

	@Test
	public void runCommand() throws Exception {
		this.cli.run("command", "--arg1", "arg2");
		verify(this.regularCommand).run("--arg1", "arg2");
	}

	@Test
	public void missingCommand() throws Exception {
		this.thrown.expect(NoSuchCommandException.class);
		this.cli.run("missing");
	}

	@Test
	public void handlesSuccess() throws Exception {
		int status = this.cli.runAndHandleErrors("command");
		assertThat(status, equalTo(0));
		assertThat(this.calls, equalTo((Set<Call>) EnumSet.noneOf(Call.class)));
	}

	@Test
	public void handlesNoArgumentsException() throws Exception {
		int status = this.cli.runAndHandleErrors();
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set<Call>) EnumSet.of(Call.SHOW_USAGE)));
	}

	@Test
	public void handlesNoSuchCommand() throws Exception {
		int status = this.cli.runAndHandleErrors("missing");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set<Call>) EnumSet.of(Call.ERROR_MESSAGE)));
	}

	@Test
	public void handlesRegularException() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run();
		int status = this.cli.runAndHandleErrors("command");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set<Call>) EnumSet.of(Call.ERROR_MESSAGE)));
	}

	@Test
	public void handlesExceptionWithDashD() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run();
		int status = this.cli.runAndHandleErrors("command", "-d");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set<Call>) EnumSet.of(Call.ERROR_MESSAGE,
				Call.PRINT_STACK_TRACE)));
	}

	@Test
	public void handlesExceptionWithDashDashDebug() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run();
		int status = this.cli.runAndHandleErrors("command", "--debug");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set<Call>) EnumSet.of(Call.ERROR_MESSAGE,
				Call.PRINT_STACK_TRACE)));
	}

	@Test
	public void exceptionMessages() throws Exception {
		assertThat(new NoSuchCommandException("name").getMessage(),
				equalTo(SpringCli.CLI_APP + ": 'name' is not a valid command. See '"
						+ SpringCli.CLI_APP + " help'."));
	}

	@Test
	public void help() throws Exception {
		this.cli.run("help", "command");
		verify(this.regularCommand).getHelp();
	}

	@Test
	public void helpNoCommand() throws Exception {
		this.thrown.expect(NoHelpCommandArgumentsException.class);
		this.cli.run("help");
	}

	@Test
	public void helpUnknownCommand() throws Exception {
		this.thrown.expect(NoSuchCommandException.class);
		this.cli.run("help", "missing");
	}

	private static enum Call {
		SHOW_USAGE, ERROR_MESSAGE, PRINT_STACK_TRACE
	}
}
