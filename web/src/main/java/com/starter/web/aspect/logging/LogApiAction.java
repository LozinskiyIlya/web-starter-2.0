package com.starter.web.aspect.logging;

import com.starter.web.aspect.logging.extractor.CurrentUserExtractor;
import com.starter.web.aspect.logging.extractor.UserExtractor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface LogApiAction {

    boolean logParams() default false;

    Class<? extends UserExtractor> userExtractor() default CurrentUserExtractor.class;
}