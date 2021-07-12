package com.cpipdm.sap.auth

import com.cpipdm.sap.configuration.Configuration
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.authentication.DefaultAuthorizationExceptionHandler
import io.micronaut.views.ViewsRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Replaces(DefaultAuthorizationExceptionHandler.class)
@Singleton
class AuthorizationHandler extends DefaultAuthorizationExceptionHandler {
    ViewsRenderer viewsRenderer
    Configuration configuration

    @Inject
    AuthorizationHandler(ViewsRenderer viewsRenderer,
                         Configuration configuration,
                         ApplicationContext applicationContext) {
        this.viewsRenderer = viewsRenderer
        this.configuration = configuration
    }

    MutableHttpResponse<?> httpResponseWithStatus(HttpRequest request, AuthorizationException exception) {
        MutableHttpResponse httpResponse = super.httpResponseWithStatus(request, exception)
        if (exception.getAuthentication() == null) {
            /**
             * General access - someone just opened the link.
             */
            return httpResponse.status(HttpStatus.FORBIDDEN)
                    .body(this.viewsRenderer
                            .render('app', ['model'  : [:],
                                            'tenant' : configuration.getTenant().keySet(),
                                            'message': 'Unauthorized access']))
                    .contentType(MediaType.TEXT_HTML)
        } else {
            /**
             * Invalid login access.
             */
            return httpResponse.status(HttpStatus.FORBIDDEN)
                    .body(this.viewsRenderer
                            .render('app', ['model'  : [:],
                                            'tenant' : configuration.getTenant().keySet(),
                                            'message': "${exception.getAuthentication().getName()} is not authorized"]))
                    .contentType(MediaType.TEXT_HTML)
        }
    }
}