package org.springframework.boot.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;

import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.springframework.boot.cli.SpringCli;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin
 * @author Dave Syer
 */
public class ShellCommand extends AbstractCommand {

	private static final String DEFAULT_PROMPT = "$ ";
	private SpringCli springCli;
	private String prompt = DEFAULT_PROMPT;
	private Stack<String> prompts = new Stack<String>();

	public ShellCommand(SpringCli springCli) {
		super("shell", "Start a nested shell (REPL).");
		this.springCli = springCli;
	}

	@Override
	public void run(String... args) throws Exception {

		final ConsoleReader console = new ConsoleReader();
		console.addCompleter(new CommandCompleter(console, this.springCli));
		console.setHistoryEnabled(true);
		console.setCompletionHandler(new CandidateListCompletionHandler());

		final InputStream sysin = System.in;
		final PrintStream sysout = System.out;
		final PrintStream syserr = System.err;

		printBanner();

		System.setIn(console.getInput());
		PrintStream out = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				console.getOutput().write(b);
			}
		});
		System.setOut(out);
		System.setErr(out);

		String line;
		StringBuffer data = new StringBuffer();

		try {

			while (null != (line = console.readLine(this.prompt))) {
				if ("quit".equals(line.trim())) {
					break;
				}
				else if ("clear".equals(line.trim())) {
					console.clearScreen();
					continue;
				}
				List<String> parts = new ArrayList<String>();

				if (line.contains("<<")) {
					int startMultiline = line.indexOf("<<");
					data.append(line.substring(startMultiline + 2));
					String contLine;
					while (null != (contLine = console.readLine("... "))) {
						if ("".equals(contLine.trim())) {
							break;
						}
						data.append(contLine);
					}
					line = line.substring(0, startMultiline);
				}

				String lineToParse = line.trim();
				if (lineToParse.startsWith("!")) {
					lineToParse = lineToParse.substring(1).trim();
				}
				String[] segments = StringUtils.delimitedListToStringArray(lineToParse,
						" ");
				StringBuffer sb = new StringBuffer();
				boolean swallowWhitespace = false;
				for (String s : segments) {
					if ("".equals(s)) {
						continue;
					}
					if (s.startsWith("\"")) {
						swallowWhitespace = true;
						sb.append(s.substring(1));
					}
					else if (s.endsWith("\"")) {
						swallowWhitespace = false;
						sb.append(" ").append(s.substring(0, s.length() - 1));
						parts.add(sb.toString());
						sb = new StringBuffer();
					}
					else {
						if (!swallowWhitespace) {
							parts.add(s);
						}
						else {
							sb.append(" ").append(s);
						}
					}
				}
				if (sb.length() > 0) {
					parts.add(sb.toString());
				}
				if (data.length() > 0) {
					parts.add(data.toString());
					data = new StringBuffer();
				}

				if (parts.size() > 0) {
					if (line.trim().startsWith("!")) {
						try {
							ProcessBuilder pb = new ProcessBuilder(parts);
							if (isJava7()) {
								inheritIO(pb);
							}
							pb.environment().putAll(System.getenv());
							Process process = pb.start();
							if (!isJava7()) {
								ProcessGroovyMethods.consumeProcessOutput(process,
										(OutputStream) sysout, (OutputStream) syserr);
							}
							process.waitFor();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					else {
						if (!getName().equals(parts.get(0))) {
							this.springCli.runAndHandleErrors(parts
									.toArray(new String[parts.size()]));
						}
					}
				}
			}

		}
		finally {

			System.setIn(sysin);
			System.setOut(sysout);
			System.setErr(syserr);

			console.shutdown();

		}
	}

	private void printBanner() {
		String version = ShellCommand.class.getPackage().getImplementationVersion();
		version = (version == null ? "" : " (v" + version + ")");
		System.out.println("Spring Boot CLI" + version);
		System.out.println("Hit TAB to complete. Type 'help' and hit RETURN for help.");
	}

	public void pushPrompt(String prompt) {
		this.prompts.push(this.prompt);
		this.prompt = prompt;
	}

	public String popPrompt() {
		if (this.prompts.isEmpty()) {
			this.prompt = DEFAULT_PROMPT;
		}
		else {
			this.prompt = this.prompts.pop();
		}
		return this.prompt;
	}

	private void inheritIO(ProcessBuilder pb) {
		ReflectionUtils.invokeMethod(
				ReflectionUtils.findMethod(ProcessBuilder.class, "inheritIO"), pb);
	}

	private boolean isJava7() {
		if (ReflectionUtils.findMethod(ProcessBuilder.class, "inheritIO") != null) {
			return true;
		}
		return false;
	}

}
