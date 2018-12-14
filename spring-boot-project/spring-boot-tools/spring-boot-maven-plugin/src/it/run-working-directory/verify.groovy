import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
def workDir = new File(basedir, "src/main/java").getAbsolutePath()
assertTrue file.text.contains("I haz been run from ${workDir}")

