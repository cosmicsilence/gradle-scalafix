package io.github.cosmicsilence.scalafix

abstract class ScalafixProps {

    private static final String PROPS_FILE_PATH = "scalafix-interfaces.properties"
    private static final Properties PROPS = new Properties()

    static {
        def file = ScalafixProps.classLoader.getResourceAsStream(PROPS_FILE_PATH)
        assert file != null, "File ${PROPS_FILE_PATH} not found in the classpath"
        PROPS.load(file)
    }

    private ScalafixProps() {}

    static String getScalafixVersion() {
        return PROPS.getProperty("scalafixVersion")
    }

    static String getScalametaVersion() {
        return PROPS.getProperty("scalametaVersion")
    }

    static String getSupportedScala211Version() {
        return PROPS.getProperty("scala211")
    }

    static String getSupportedScala212Version() {
        return PROPS.getProperty("scala212")
    }

    static String getSupportedScala213Version() {
        return PROPS.getProperty("scala213")
    }

    static String getSemanticDbArtifactCoordinates(String scalaVersion) {
        return "org.scalameta:semanticdb-scalac_${scalaVersion}:${scalametaVersion}"
    }
}