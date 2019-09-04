import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue file.text.contains("I haz been run from '$basedir'")
assertTrue file.text.contains("JVM argument(s): -Xverify:none -XX:TieredStopAtLevel=1")

