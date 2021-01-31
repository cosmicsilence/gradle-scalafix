package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException

abstract class ScalafixProperties {

    private static final String SCALAFIX_PROPS_PATH = "scalafix-interfaces.properties"
    private static final Properties SCALAFIX_PROPS = new Properties()

    static {
        def file = ScalafixProperties.classLoader.getResourceAsStream(SCALAFIX_PROPS_PATH)
        assert file != null, "File ${SCALAFIX_PROPS_PATH} not found in the classpath"
        SCALAFIX_PROPS.load(file)
    }

    private ScalafixProperties() {}

    static String getScalafixVersion() {
        SCALAFIX_PROPS.getProperty("scalafixVersion")
    }

    static String getScalametaVersion() {
        SCALAFIX_PROPS.getProperty("scalametaVersion")
    }

    static String getSupportedScala211Version() {
        SCALAFIX_PROPS.getProperty("scala211")
    }

    static String getSupportedScala212Version() {
        SCALAFIX_PROPS.getProperty("scala212")
    }

    static String getSupportedScala213Version() {
        SCALAFIX_PROPS.getProperty("scala213")
    }

    static String supportedScalaVersion(String scalaVersion) {
        String supportedScalaVersion

        switch (scalaVersion) {
            case ~/^2\.11\..+$/:
                supportedScalaVersion = supportedScala211Version
                break
            case ~/^2\.12\..+$/:
                supportedScalaVersion = supportedScala212Version
                break
            case ~/^2\.13\..+$/:
                supportedScalaVersion = supportedScala213Version
                break
            default:
                throw new GradleException("Unsupported Scala version. Scalafix supports only 2.11, 2.12 and 2.13")
        }
        supportedScalaVersion
    }

}