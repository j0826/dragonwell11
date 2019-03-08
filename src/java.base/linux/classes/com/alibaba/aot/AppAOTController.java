package com.alibaba.aot;
import java.lang.IllegalArgumentException;

public class AppAOTController {
    /* Make sure registerNatives is the first thing <clinit> does. */
    private static native void registerNatives();

    static {
        registerNatives();
    }

    public static synchronized int loadAOTLibraryForLoader(ClassLoader loader, String library) {
        if ( loader == null || library == null ) {
            throw new IllegalArgumentException("loader or library can not be null");
        } else if (library.length() == 0) {
            throw new IllegalArgumentException("library name can not be empty");
        } else {
            return loadAOTLibraryForLoader0(loader, library);
        }
    }

    public static synchronized void unloadAOTLibraryForLoader(ClassLoader loader) {
        if ( loader != null ) {
            unloadAOTLibraryForLoader0(loader);
        }
        return;
    }

    private static native int loadAOTLibraryForLoader0(ClassLoader loader, String library);
    private static native void unloadAOTLibraryForLoader0(ClassLoader loader);
}
