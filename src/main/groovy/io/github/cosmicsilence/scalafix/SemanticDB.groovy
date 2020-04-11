package io.github.cosmicsilence.scalafix

abstract class SemanticDB {

    private static final String SCALAFIX_PROPS_PATH = "scalafix-interfaces.properties"
    private static final Properties SCALAFIX_PROPS = new Properties()

    static {
        def file = SemanticDB.classLoader.getResourceAsStream(SCALAFIX_PROPS_PATH)
        assert file != null, "File ${SCALAFIX_PROPS_PATH} not found in the classpath"
        SCALAFIX_PROPS.load(file)
    }

    private SemanticDB() {}

    static String getScalametaVersion() {
        SCALAFIX_PROPS.getProperty("scalametaVersion")
    }

    static String getSupportedScala211Version() {
        SCALAFIX_PROPS.getProperty("scala211")
    }

    static String getSupportedScala212Version() {
        SCALAFIX_PROPS.getProperty("scala212")
    }

    static Optional<String> getMavenCoordinates(String scalaVersion) {
        String supportedScalaVersion

        switch (scalaVersion) {
            case ~/^2\.11\..+$/:
                supportedScalaVersion = supportedScala211Version
                break
            case ~/^2\.12\..+$/:
                supportedScalaVersion = supportedScala212Version
                break
            case ~/^2\.13\..+$/:
                // Using the project's Scala version as:
                // - semanticdb_2.13.0 is incompatible with Scala 2.13.1
                // - semanticdb_2.13.1 is incompatible with Scala 2.13.0
                supportedScalaVersion = scalaVersion
                break
            default:
                supportedScalaVersion = null
        }

        Optional.ofNullable(supportedScalaVersion).map {
            "org.scalameta:semanticdb-scalac_$it:$scalametaVersion"
        }
    }
}