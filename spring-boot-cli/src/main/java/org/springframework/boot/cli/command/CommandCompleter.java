package org.springframework.boot.cli.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.Log;
import org.springframework.boot.cli.OptionHelp;
import org.springframework.boot.cli.SpringCli;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin
 */
public class CommandCompleter extends StringsCompleter {

	private final Map<String, Completer> optionCompleters = new HashMap<String, Completer>();
	private List<Command> commands = new ArrayList<Command>();
	private ConsoleReader console;
	private String lastBuffer;

	public CommandCompleter(ConsoleReader console, SpringCli cli) {
		this.console = console;

		this.commands.addAll(cli.getCommands());
		List<String> names = new ArrayList<String>();
		for (Command c : this.commands) {
			names.add(c.getName());
			List<String> opts = new ArrayList<String>();
			for (OptionHelp optHelp : c.getOptionsHelp()) {
				opts.addAll(optHelp.getOptions());
			}
			this.optionCompleters.put(c.getName(), new ArgumentCompleter(
					new StringsCompleter(c.getName()), new StringsCompleter(opts),
					new NullCompleter()));
		}
		getStrings().addAll(names);

	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		int i = super.complete(buffer, cursor, candidates);
		if (buffer.indexOf(' ') < 1) {
			return i;
		}
		String name = buffer.substring(0, buffer.indexOf(' '));
		if ("".equals(name.trim())) {
			return i;
		}
		for (Command c : this.commands) {
			if (!c.getName().equals(name)) {
				continue;
			}
			if (buffer.equals(this.lastBuffer)) {
				this.lastBuffer = buffer;
				try {
					this.console.println();
					this.console.println("Usage:");
					this.console.println(c.getName() + " " + c.getUsageHelp());
					List<List<String>> rows = new ArrayList<List<String>>();
					int maxSize = 0;
					for (OptionHelp optHelp : c.getOptionsHelp()) {
						List<String> cols = new ArrayList<String>();
						for (String s : optHelp.getOptions()) {
							cols.add(s);
						}
						String opts = StringUtils
								.collectionToDelimitedString(cols, " | ");
						if (opts.length() > maxSize) {
							maxSize = opts.length();
						}
						cols.clear();
						cols.add(opts);
						cols.add(optHelp.getUsageHelp());
						rows.add(cols);
					}

					StringBuilder sb = new StringBuilder("\t");
					for (List<String> row : rows) {
						String col1 = row.get(0);
						String col2 = row.get(1);
						for (int j = 0; j < (maxSize - col1.length()); j++) {
							sb.append(" ");
						}
						sb.append(col1).append(": ").append(col2);
						this.console.println(sb.toString());
						sb = new StringBuilder("\t");
					}

					this.console.drawLine();
				}
				catch (IOException e) {
					Log.error(e.getMessage() + " (" + e.getClass().getName() + ")");
				}
			}
			Completer completer = this.optionCompleters.get(c.getName());
			if (null != completer) {
				i = completer.complete(buffer, cursor, candidates);
				break;
			}
		}

		this.lastBuffer = buffer;
		return i;
	}

}
