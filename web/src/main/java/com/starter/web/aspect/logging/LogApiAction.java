package com.starter.web.aspect.logging;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
public @interface LogApiAction {

    boolean logParams() default false;

    Class<? extends UserExtractor> userExtractor() default CurrentUserExtractor.class;
}