"""Check the source anchors used by REPORT.md against this checkout."""

from pathlib import Path


SOURCE = Path(__file__).parents[3] / "core" / "spring-boot" / "src" / "main" / "java" / "org" / "springframework" / "boot" / "SpringApplication.java"
ANCHORS = (
    "public ConfigurableApplicationContext run(String... args)",
    "if (this.properties.isRegisterShutdownHook())",
    "if (!this.isCustomEnvironment)",
    "protected void configureEnvironment",
    "protected void configurePropertySources",
    "for (ApplicationContextInitializer initializer : getInitializers())",
    "for (String beanName : beanNames)",
    "for (SpringBootExceptionReporter reporter : exceptionReporters)",
    "while (true)",
)


def main():
    source = SOURCE.read_text(encoding="utf-8").splitlines()
    missing = [anchor for anchor in ANCHORS if not any(anchor in line for line in source)]
    if missing:
        raise SystemExit("Missing source anchors: " + ", ".join(missing))
    print(f"Verified {len(ANCHORS)} SpringApplication source anchors in {SOURCE}")


if __name__ == "__main__":
    main()
