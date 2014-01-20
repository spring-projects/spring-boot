package org.springframework.boot.cli.it;

import java.io.IOException;

import org.junit.Test;
import org.springframework.boot.cli.it.infrastructure.Cli;
import org.springframework.boot.cli.it.infrastructure.CliInvocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andy Wilkinson
 */
public class UsabilityTests {

	private final Cli cli = new Cli();

	@Test
	public void hintProducesListOfValidCommands() throws IOException,
			InterruptedException {
		CliInvocation cli = this.cli.invoke("hint");

		assertEquals(0, cli.await());
		assertEquals(0, cli.getErrorOutput().length());
		assertEquals(6, cli.getStandardOutputLines().size());
	}

	@Test
	public void invokingWithNoArgumentsDisplaysHelp() throws IOException,
			InterruptedException {
		CliInvocation cli = this.cli.invoke();

		assertEquals(1, cli.await()); // TODO Should this be 0? Probably not.
		assertEquals(0, cli.getErrorOutput().length());
		assertTrue(cli.getStandardOutput().startsWith("usage:"));
	}

	@Test
	public void unrecognizedCommandsAreHandledGracefully() throws IOException,
			InterruptedException {
		CliInvocation cli = this.cli.invoke("not-a-real-command");

		assertEquals(1, cli.await());
		assertTrue(
				cli.getErrorOutput(),
				cli.getErrorOutput().contains(
						"'not-a-real-command' is not a valid command"));
		assertEquals(0, cli.getStandardOutput().length());
	}

	@Test
	public void version() throws IOException, InterruptedException {
		CliInvocation cli = this.cli.invoke("version");

		assertEquals(0, cli.await());
		assertEquals(0, cli.getErrorOutput().length());
		assertTrue(cli.getStandardOutput(),
				cli.getStandardOutput().startsWith("Spring CLI v"));
	}

	@Test
	public void help() throws IOException, InterruptedException {
		CliInvocation cli = this.cli.invoke("help");

		assertEquals(1, cli.await()); // TODO Should this be 0? Perhaps.
		assertEquals(0, cli.getErrorOutput().length());
		assertTrue(cli.getStandardOutput().startsWith("usage:"));
	}
}
