import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue file.text.contains("I haz been run")
assertTrue file.text.contains("Fork mode disabled, ignoring JVM argument(s) [-Dfoo=bar]")

