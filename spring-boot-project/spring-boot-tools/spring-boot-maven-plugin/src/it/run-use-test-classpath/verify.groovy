def file = new File(basedir, "build.log")
return file.text.contains("I haz been run")

