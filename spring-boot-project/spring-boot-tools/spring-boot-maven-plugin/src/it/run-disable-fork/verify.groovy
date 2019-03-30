import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue file.text.contains("I haz been run")
assertTrue file.text.contains("Fork mode disabled, ignoring JVM argument(s) [-Dproperty1=value1 -Dproperty2 -Dfoo=bar]")
assertTrue file.text.contains("Fork mode disabled, ignoring working directory configuration")

