import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue file.text.contains("Layout: ZIP")
