package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import scalafix.interfaces.Scalafix
import scalafix.internal.interfaces.ScalafixInterfacesClassloader

import java.util.concurrent.ConcurrentHashMap

abstract class Classloaders {

    private static final Logger logger = Logging.getLogger(Classloaders)
    private static final ConcurrentHashMap<Object, ClassLoader> cache = new ConcurrentHashMap<>()

    private Classloaders() {}

    static ClassLoader forScalafixCli(Project project, String scalafixCliCoordinates) {
        return cache.computeIfAbsent(scalafixCliCoordinates, {
            logger.debug("Creating classloader for '${scalafixCliCoordinates}'")
            def cliDependency = project.dependencies.create(scalafixCliCoordinates)
            def cliConfiguration = project.configurations.detachedConfiguration(cliDependency)
            def parentClassloader = new ScalafixInterfacesClassloader(Scalafix.class.classLoader)
            classloaderFrom(cliConfiguration, parentClassloader)
        })
    }

    static URLClassLoader forExternalRules(Configuration rulesConfiguration, ClassLoader scalafixCliClassloader) {
        // No cache in this case as rules can be loaded from a subproject or source set under the same project.
        // There is no guarantee that rules would not be modified between executions.
        return classloaderFrom(rulesConfiguration, scalafixCliClassloader)
    }

    private static URLClassLoader classloaderFrom(Configuration configuration, ClassLoader parent) {
        def jarFiles = configuration.collect { it.toURI().toURL() }.toArray(new URL[0])
        return new URLClassLoader(jarFiles, parent)
    }
}
