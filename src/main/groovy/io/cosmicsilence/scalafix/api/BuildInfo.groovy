package io.cosmicsilence.scalafix.api

abstract class BuildInfo {

    private static final String PATH = "scalafix-interfaces.properties"
    private static final Properties PROPERTIES = new Properties()

    static {
        PROPERTIES.load(BuildInfo.classLoader.getResourceAsStream(PATH))
    }

    private BuildInfo() {
    }

    static String getScalafixVersion() {
        PROPERTIES.getProperty("scalafixVersion")
    }

    static String getScala212Version() {
        PROPERTIES.getProperty("scala212")
    }
}