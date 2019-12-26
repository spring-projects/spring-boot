def file = new File(basedir, "build.log")
return file.text.contains("The Maven Toolchains is awesome!")
