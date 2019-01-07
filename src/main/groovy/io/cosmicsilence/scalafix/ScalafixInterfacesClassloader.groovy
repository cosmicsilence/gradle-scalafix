package io.cosmicsilence.scalafix

/**
 * A classloader that shares only scalafix-interfaces classes from the parent classloader.
 *
 * This classloader is intended to be used as a parent when class-loading scalafix-cli.
 * By using this classloader as a parent, it's possible to cast runtime instances from
 * the scalafix-cli classloader into `scalafix.interfaces.Scalafix` from this classloader.
 */
class ScalafixInterfacesClassloader extends ClassLoader {

    private final ClassLoader _parent

    ScalafixInterfacesClassloader(ClassLoader parent) {
        super(null)
        _parent = parent
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("scalafix.interfaces.")) _parent.loadClass(name)
        else throw new ClassNotFoundException(name)
    }
}
