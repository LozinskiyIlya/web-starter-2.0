package com.starter.web.aspect.logging.extractor;

abstract class StringParameterUserExtractor extends ParameterUserExtractor<String> {

    @Override
    Class<String> getParameterType() {
        return String.class;
    }

}
