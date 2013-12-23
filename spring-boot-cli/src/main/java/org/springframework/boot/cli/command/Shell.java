package org.springframework.boot.cli.command;

import java.io.IOException;

import org.springframework.boot.cli.SpringCli;

/**
 * @author Dave Syer
 */
public class Shell {

	public static void main(String... args) throws IOException {
		if (args.length == 0) {
			SpringCli.main("shell"); // right into the REPL by default
		} else {
			SpringCli.main(args);
		}
	}

}
