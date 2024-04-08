package com.starter.common.aspect.logging.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

/**
 * Non-abstract implementations must keep naming convention <ParameterName>UserExtractor
 * {@link ParameterUserExtractor#getParameterNameFromClass}
 * ATTENTION: This implementation works only with -parameter compiler flag
 * For details: see pom of the root module and <a href="https://docs.oracle.com/javase/tutorial/reflect/member/methodparameterreflection.html">java doc</a>
 */

@Component
abstract class ParameterUserExtractor<T> implements UserExtractor {

    private final String parameterName = getParameterNameFromClass();

    abstract Class<T> getParameterType();

    abstract UserQualifier getUserQualifier(T parameter);

    @Override
    public UserQualifier extract(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        final var methodSignature = (MethodSignature) joinPoint.getSignature();
        final var args = joinPoint.getArgs(); // Instances of the method arguments
        final var parameterTypes = methodSignature.getMethod().getParameterTypes();  // Types of the method arguments
        final var parameterNames = methodSignature.getParameterNames();
        final var value = findValue(args, parameterTypes, parameterNames);
        if (value == null) {
            return UserQualifier.empty();
        }
        return getUserQualifier(value);
    }

    private T findValue(Object[] args, Class<?>[] parameterTypes, String[] parameterNames) {
        for (int i = 0; i < args.length; i++) {
            if (parameterTypes[i].equals(getParameterType()) && parameterNames[i].equalsIgnoreCase(parameterName)) {
                return getParameterType().cast(args[i]);
            }
            // Attempt to find a value via getters
            final var valueFromGetter = getValueFromGetter(args[i]);
            if (valueFromGetter != null) {
                return valueFromGetter;
            }

            // Fallback to field access if getter not found (optional, consider the implications)
            final var valueFromField = getValueFromField(args[i]);
            if (valueFromField != null) {
                return valueFromField;
            }
        }
        return null;
    }

    private T getValueFromGetter(Object arg) {
        try {
            // Build getter method name for 'parameterName'
            final var getter = arg.getClass().getMethod("get" + parameterName);
            // Invoke the getter if it exists
            final var returnValue = getter.invoke(arg);
            return getParameterType().cast(returnValue);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Getter method not found or not accessible, ignore and move on
        }
        return null;
    }

    private T getValueFromField(Object arg) {
        try {
            final var field = arg.getClass().getDeclaredField(parameterName.toLowerCase());
            field.setAccessible(true); // Make private field accessible
            final var fieldValue = field.get(arg);
            return getParameterType().cast(fieldValue);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Field not found or not accessible, ignore and move on
        }
        return null;
    }

    private String getParameterNameFromClass() {
        final var className = this.getClass().getSimpleName();
        // Get first word
        return className.substring(0, className.indexOf("UserExtractor"));
    }
}
