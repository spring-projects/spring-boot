package org.springframework.boot.cli.it.infrastructure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andy Wilkinson
 */
public final class Cli {

	public CliInvocation invoke(String... args) throws IOException {
		return new CliInvocation(launchCli(args));
	}

	private Process launchCli(String... args) throws IOException {
		List<String> arguments = Arrays.asList(args);

		List<String> command = new ArrayList<String>();
		command.add(findLaunchScript().getAbsolutePath());
		command.addAll(arguments);

		return new ProcessBuilder(command).start();
	}

	private File findLaunchScript() {
		File dependencyDirectory = new File("target/dependency");
		if (dependencyDirectory.isDirectory()) {
			File[] files = dependencyDirectory.listFiles();
			if (files.length == 1) {
				File binDirectory = new File(files[0], "bin");
				if (isWindows()) {
					return new File(binDirectory, "spring.bat");
				}
				else {
					return new File(binDirectory, "spring");
				}
			}
		}
		throw new IllegalStateException("Could not find CLI launch script");
	}

	private boolean isWindows() {
		return File.separatorChar == '\\';
	}
}
