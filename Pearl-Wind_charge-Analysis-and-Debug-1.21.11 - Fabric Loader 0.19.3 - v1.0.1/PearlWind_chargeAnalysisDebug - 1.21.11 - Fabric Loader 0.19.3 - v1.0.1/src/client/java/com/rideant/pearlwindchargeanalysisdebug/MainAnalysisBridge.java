package com.rideant.pearlwindchargeanalysisdebug;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Keeps this companion source build independent from a bundled main-mod JAR.
 */
public final class MainAnalysisBridge {
    private static final String PROVIDER_CLASS =
            "com.example.pearlwindchargeanalysis.AimTargetProvider";

    private static Method resolveMethod;
    private static boolean lookupAttempted;
    private static boolean failureLogged;

    private MainAnalysisBridge() {
    }

    public static double[] resolveAimTarget(int index) {
        Method method = getResolveMethod();
        if (method == null) {
            return null;
        }

        try {
            Object result = method.invoke(null, index);
            if (result instanceof double[] values && values.length >= 2) {
                return values;
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            logFailure(exception);
        }
        return null;
    }

    private static Method getResolveMethod() {
        if (lookupAttempted) {
            return resolveMethod;
        }

        lookupAttempted = true;
        try {
            Class<?> provider = Class.forName(PROVIDER_CLASS);
            resolveMethod = provider.getMethod("resolve", int.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            logFailure(exception);
        }
        return resolveMethod;
    }

    private static void logFailure(Exception exception) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        PearlWindChargeAnalysisDebug.LOGGER.warn(
                "Unable to access PearlWind_chargeAnalysis aim API",
                exception
        );
    }
}
