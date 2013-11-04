package crash.commands.base;

import org.crsh.cli.Argument;
import org.crsh.cli.Command;
import org.crsh.cli.Option;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;
import org.crsh.command.InvocationContext;
import org.crsh.command.PipeCommand;
import org.crsh.command.ScriptException;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author Julien Viet */
@Usage("Java Management Extensions")
public class jmx extends BaseCommand {

  @Usage("find mbeans")
  @Command
  public void find(
      InvocationContext<ObjectName> context,
      @Usage("The object name pattern")
      @Option(names = {"p", "pattern"})
      String pattern) throws Exception {

    //
    ObjectName patternName = pattern != null ? ObjectName.getInstance(pattern) : null;
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectInstance> instances = server.queryMBeans(patternName, null);
    for (ObjectInstance instance : instances) {
      context.provide(instance.getObjectName());
    }
/*
    if (context.piped) {
    } else {
      UIBuilder ui = new UIBuilder()
      ui.table(columns: [1,3]) {
        row(bold: true, fg: black, bg: white) {
          label("CLASS NAME"); label("OBJECT NAME")
        }
        instances.each { instance ->
          row() {
            label(foreground: red, instance.getClassName()); label(instance.objectName)
          }
        }
      }
      out << ui;
    }
*/
  }

  @Command
  @Usage("return the attributes info of an MBean")
  public void attributes(InvocationContext<Map> context, @Argument ObjectName name) throws IOException {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      MBeanInfo info = server.getMBeanInfo(name);
      for (MBeanAttributeInfo attributeInfo : info.getAttributes()) {
        HashMap<String, Object> tuple = new HashMap<String, Object>();
        tuple.put("name", attributeInfo.getName());
        tuple.put("type", attributeInfo.getType());
        tuple.put("description", attributeInfo.getDescription());
        context.provide(tuple);
      }
    }
    catch (JMException e) {
      throw new ScriptException("Could not access MBean meta data", e);
    }
  }

  @Usage("get attributes of an MBean")
  @Command
  public PipeCommand<ObjectName, Map> get(@Argument final List<String> attributes) {

    // Determine common attributes from all names
    if (attributes == null || attributes.isEmpty()) {
      throw new ScriptException("Must provide JMX attributes");
    }

    //
    return new PipeCommand<ObjectName, Map>() {

      /** . */
      private MBeanServer server;

      @Override
      public void open() throws ScriptException {
        server = ManagementFactory.getPlatformMBeanServer();
      }

      @Override
      public void provide(ObjectName name) throws IOException {
        try {
          HashMap<String, Object> tuple = new HashMap<String, Object>();
          for (String attribute : attributes) {
            String prop = name.getKeyProperty(attribute);
            if (prop != null) {
              tuple.put(attribute, prop);
            }
            else {
              tuple.put(attribute, server.getAttribute(name, attribute));
            }
          }
          context.provide(tuple);
        }
        catch (JMException ignore) {
          //
        }
      }
    };
  }
}
