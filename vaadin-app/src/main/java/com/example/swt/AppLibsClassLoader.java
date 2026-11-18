package com.example.swt;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import com.vaadin.swingbridge.swt.SwtBridge;
import com.vaadin.swingbridge.swt.internal.SwtBridgeSettings;

/**
 * Class loader for one session's copy of the SWT application, loading the
 * {@code applibs} jars child-first.
 * <p>
 * The host has {@code warehouse-core} on its own class path (the login and
 * the modern dashboard use it), and the application jar contains the same
 * classes. With the usual parent-first delegation every session would get the
 * host's copies, and so share {@code AppSession}'s current user and one
 * Hibernate factory. Loading the application's own jars first gives each
 * session its own.
 * <p>
 * Only the platform stays shared: the JDK, SWT, the bridge, Swing Bridge's
 * interop annotations, and the interop types in
 * {@code com.example.warehouse.interop} that Vaadin views and the application
 * pass to each other. The parent is the bridge's SWT class loader, which has
 * SWT and, through its own parent, the host's classes.
 */
public final class AppLibsClassLoader extends URLClassLoader {

    private static final List<String> PARENT_FIRST = List.of("java.", "javax.",
            "jdk.", "sun.", "com.sun.", "org.eclipse.swt.",
            "com.vaadin.swingbridge.",
            // Types the host and the application share through interop (in
            // warehouse-core): the listener interfaces and events Vaadin views
            // receive must be one class on both sides.
            "com.example.warehouse.interop.");

    static {
        registerAsParallelCapable();
    }

    private AppLibsClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /** Supplies a fresh loader over the jars in {@code applibs}, per session. */
    public static Supplier<URLClassLoader> fromAppLibs() {
        return () -> {
            List<Path> jars = SwtBridgeSettings.appLibJars();
            URL[] urls = jars.stream().map(AppLibsClassLoader::toUrl)
                    .toArray(URL[]::new);
            return new AppLibsClassLoader(urls, SwtBridge.swtClassLoader());
        };
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(path.toString(), e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if (PARENT_FIRST.stream().anyMatch(name::startsWith)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try {
                    loaded = findClass(name);
                } catch (ClassNotFoundException notInAppLibs) {
                    loaded = super.loadClass(name, false);
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    @Override
    public URL getResource(String name) {
        // Child-first for resources too: persistence.xml.
        URL url = findResource(name);
        return url != null ? url : super.getResource(name);
    }
}
