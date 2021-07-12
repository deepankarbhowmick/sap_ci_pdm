package com.cpipdm.sap.controller

import com.cpipdm.sap.configuration.Configuration
import com.cpipdm.sap.model.Login
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.filters.SecurityFilter
import io.micronaut.security.rules.SecurityRule
import io.micronaut.session.Session
import io.micronaut.views.View
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.annotations.NonNull
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@Controller('/')
class Index {
    Login login
    Configuration configuration

    @Inject
    Index(Login login, Configuration configuration) {
        this.login = login
        this.configuration = configuration
    }

    @Get(produces = MediaType.TEXT_HTML)
    @Secured(SecurityRule.IS_ANONYMOUS)
    Single<MutableHttpResponse<Map>> index(Session session,
                                           @Nullable @CookieValue('credential') String cookie) {
        if ((session.get(SecurityFilter.AUTHENTICATION) as Optional).isPresent()) {
            /**
             * Already logged in - redirect to dashboard directly. Return a single as flowable.
             */
            return Single.<MutableHttpResponse> create(new SingleOnSubscribe<MutableHttpResponse>() {
                @Override
                void subscribe(@NonNull SingleEmitter<MutableHttpResponse> emitter) throws Exception {
                    emitter.onSuccess(MutableHttpResponse.permanentRedirect(new URI('/login')))
                }
            }).subscribeOn(Schedulers.io())
        } else {
            /**
             * Show login.
             */
            return this.login.readFromCookie(cookie)
        }
    }

    @Post(value = '/customLogin', consumes = MediaType.APPLICATION_FORM_URLENCODED)
    @Secured(SecurityRule.IS_ANONYMOUS)
    Single<MutableHttpResponse> login(@Body Map login) {
        return this.login.saveToCookie(login)
    }

    @Error
    @View('app')
    MutableHttpResponse<Map> handleLocalError(HttpRequest httpRequest, Exception exception) {
        return MutableHttpResponse.ok(['model'  : [:],
                                       'tenant' : configuration.getTenant().keySet(),
                                       'message': exception.getMessage()])
    }
}
