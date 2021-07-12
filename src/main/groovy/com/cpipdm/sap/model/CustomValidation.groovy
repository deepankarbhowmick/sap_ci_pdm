package com.cpipdm.sap.model

import io.micronaut.aop.Around
import io.micronaut.context.annotation.Bean

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import static java.lang.annotation.RetentionPolicy.RUNTIME

@Documented
@Retention(RUNTIME)
@Around
@interface CustomValidation {
    boolean isValidEmail()
}