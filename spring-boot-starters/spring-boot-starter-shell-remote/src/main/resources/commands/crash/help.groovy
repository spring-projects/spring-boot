package crash.commands.base;

import org.crsh.cli.Command;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;
import org.crsh.command.DescriptionFormat;
import org.crsh.command.InvocationContext;
import org.crsh.command.ShellCommand;
import org.crsh.shell.impl.command.CRaSH;
import org.crsh.text.Color;
import org.crsh.text.Decoration;
import org.crsh.text.Style;
import org.crsh.text.ui.LabelElement;
import org.crsh.text.ui.RowElement;
import org.crsh.text.ui.TableElement;

import java.io.IOException;

/** @author Julien Viet */
public class help extends BaseCommand {

  @Usage("provides basic help")
  @Command
  public void main(InvocationContext<Object> context) throws IOException {

    //
    TableElement table = new TableElement().rightCellPadding(1);
    table.add(
        new RowElement().
            add(new LabelElement("NAME").style(Style.style(Decoration.bold))).
            add(new LabelElement("DESCRIPTION")));

    //
    CRaSH crash = (CRaSH)context.getSession().get("crash");
    Iterable<String> names = crash.getCommandNames();
    for (String name : names) {
      try {
        ShellCommand cmd = crash.getCommand(name);
        if (cmd != null) {
          String desc = cmd.describe(name, DescriptionFormat.DESCRIBE);
          if (desc == null) {
            desc = "";
          }
          table.add(
              new RowElement().
                  add(new LabelElement(name).style(Style.style(Color.red))).
                  add(new LabelElement(desc)));
        }
      } catch (Exception ignore) {
        //
      }
    }

    //
    context.provide(new LabelElement("Try one of these commands with the -h or --help switch:\n"));
    context.provide(table);
  }
}
