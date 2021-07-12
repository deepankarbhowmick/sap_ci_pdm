package com.cpipdm.sap.model

import com.cpipdm.sap.model.CustomValidation
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import javax.inject.Singleton

@Singleton
@InterceptorBean(CustomValidation.class)
class CustomValidator implements MethodInterceptor<Object, Object> {
    @Override
    Object intercept(MethodInvocationContext<Object, Object> context) {
        try {
            if ((context.getAnnotation(CustomValidation.class)).getValues().get('isValidEmail')) {
                String url = context.getParameterValueMap().get('tmn')
                new URL(url).toURI()
            }
            context.proceed()
        }
        catch (Exception exception) {
            throw exception
        }
    }
}
