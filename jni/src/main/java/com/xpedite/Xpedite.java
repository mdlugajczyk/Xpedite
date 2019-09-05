///////////////////////////////////////////////////////////////////////////////
//
// Java agent to instrument java code with Xpedite probes
//
// Author: Brooke Elizabeth Cantwell, Morgan Stanley
//
///////////////////////////////////////////////////////////////////////////////

package com.xpedite;

import com.xpedite.probes.AbstractProbe;
import com.xpedite.probes.CallSite;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class Xpedite {
    private static Instrumentation inst = null;

    public static native void record(int id);
    public static native void profile(AbstractProbe[] probes);

    private static volatile Xpedite xpediteInstance;

    private Xpedite() {
        String ldLibraryPath = System.getProperty("java.library.path");
        String[] preloadedLibraryPaths = ldLibraryPath.split(":");
        for (String libPath: preloadedLibraryPaths) {
            File lib = new File(libPath + "libXpediteJNI.so");
            if (lib.exists()) {
                System.load(String.valueOf(lib));
                return;
            }
        }
        throw new UnsatisfiedLinkError("Failed to locate Xpedite JNI shared object in library path");
    }

    public static Xpedite getInstance() {
        if (xpediteInstance == null) {
            synchronized (Xpedite .class) {
                if (xpediteInstance == null) {
                    xpediteInstance = new Xpedite();
                }
            }
        }
        return xpediteInstance;
    }

    public static void activateProbes(AbstractProbe[] probes) throws IOException {
        AppInfo.appendAppInfo(probes);
        for (AbstractProbe probe: probes) {
            for (CallSite callSite: probe.getCallSites()) {
                inst.addTransformer(new ClassTransformer(callSite), true);
            }
        }
    }

    public static void premain(String args, Instrumentation instrumentation) {
        inst = instrumentation;
    }
}
