import static org.junit.Assert.assertFalse

def file = new File(basedir, "build.log")
assertFalse file.text.contains("Layout:")
