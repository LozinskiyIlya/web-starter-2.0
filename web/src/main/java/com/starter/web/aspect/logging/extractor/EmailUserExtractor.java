package com.starter.web.aspect.logging.extractor;

import com.starter.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;


@Component
@RequiredArgsConstructor
public class EmailUserExtractor implements UserExtractor {

    private final UserRepository userRepository;

    @Override
    public UserQualifier extract(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        final var methodSignature = (MethodSignature) joinPoint.getSignature();
        final var args = joinPoint.getArgs(); // Instances of the method arguments
        final var parameterTypes = methodSignature.getMethod().getParameterTypes();  // Types of the method arguments
        final var parameterNames = methodSignature.getParameterNames();
        final var email = findEmail(args, parameterTypes, parameterNames);
        return userRepository.findByLogin(email)
                .map(user -> new UserQualifier(user.getId(), user.getLogin()))
                .orElse(new UserQualifier(null, email));
    }

    private String findEmail(Object[] args, Class<?>[] parameterTypes, String[] parameterNames) {
        for (int i = 0; i < args.length; i++) {
            if (parameterTypes[i].equals(String.class) && parameterNames[i].equals("email")) {
                return (String) args[i];
            }
            // Attempt to find an email via getters
            final var emailFromGetter = getEmailFromGetter(args[i]);
            if (StringUtils.hasText(emailFromGetter)) {
                return emailFromGetter;
            }

            // Fallback to field access if getter not found (optional, consider the implications)
            final var emailFromField = getEmailFromField(args[i]);
            if (StringUtils.hasText(emailFromField)) {
                return emailFromField;
            }
        }
        return "";
    }

    private String getEmailFromGetter(Object arg) {
        try {
            // Build getter method name for 'email'
            final var getEmailMethod = arg.getClass().getMethod("getEmail");
            // Invoke the getEmail method if it exists
            final var returnValue = getEmailMethod.invoke(arg);
            if (returnValue instanceof String) {
                return (String) returnValue;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Getter method not found or not accessible, ignore and move on
        }
        return "";
    }

    private String getEmailFromField(Object arg) {
        try {
            final var emailField = arg.getClass().getDeclaredField("email");
            emailField.setAccessible(true); // Make private field accessible
            final var fieldValue = emailField.get(arg);
            if (fieldValue instanceof String) {
                return (String) fieldValue;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Email field not found or not accessible, ignore and move on
        }
        return "";
    }
}
