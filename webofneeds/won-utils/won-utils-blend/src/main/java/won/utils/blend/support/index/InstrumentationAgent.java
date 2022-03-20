package won.utils.blend.support.index;

import java.lang.instrument.Instrumentation;

public class InstrumentationAgent {
    private static volatile Instrumentation globalInstrumentation;

    public static void premain(final String agentArgs, final Instrumentation inst) {
        globalInstrumentation = inst;
        System.out.println("Instrumentation agent loaded");
    }

    public static long getObjectSize(final Object object) {
        if (globalInstrumentation == null) {
            throw new IllegalStateException("Agent not initialized.");
        }
        return globalInstrumentation.getObjectSize(object);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        globalInstrumentation = inst;
        System.out.println("Instrumentation agent loaded");
    }
}
