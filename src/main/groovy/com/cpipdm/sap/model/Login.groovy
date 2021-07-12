package com.cpipdm.sap.model

import com.cpipdm.sap.configuration.Configuration
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.netty.cookies.NettyCookie
import io.micronaut.http.netty.cookies.NettyCookieFactory
import io.micronaut.http.netty.cookies.NettyCookies
import io.micronaut.views.ViewsRenderer
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.MaybeOnSubscribe
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.annotations.NonNull
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Login {
    Configuration configuration
    ViewsRenderer viewsRenderer

    @Inject
    Login(Configuration configuration, ViewsRenderer viewsRenderer) {
        this.configuration = configuration
        this.viewsRenderer = viewsRenderer
    }

    @CustomValidation(isValidEmail = true)
    void checkTmnUrl(String tmn) {
        // do nothing.
    }

    Single<MutableHttpResponse> readFromCookie(String cookie) {

        /**
         * Return a maybe as a flowable.
         */
        return Maybe.<MutableHttpResponse> create(new MaybeOnSubscribe<MutableHttpResponse>() {
            @Override
            void subscribe(@NonNull MaybeEmitter<MutableHttpResponse> emitter) throws Exception {
                HashMap login = new HashMap()

                if (cookie != null) {
                    login.put('username', new String(Base64.getDecoder()
                            .decode(cookie.split(':')
                                    .getAt(0) as String)))
                    login.put('password', new String(Base64.getDecoder()
                            .decode(cookie.split(':')
                                    .getAt(1) as String)))
                }
                emitter.onSuccess(MutableHttpResponse.ok(viewsRenderer.render('app', ['model' : login,
                                                                                      'tenant': configuration.getTenant().keySet()])))
                emitter.onComplete()
            }
        }).subscribeOn(Schedulers.io())
                .toSingle()
    }

    Single<MutableHttpResponse> saveToCookie(Map login) {
        Single.<MutableHttpResponse> create(new SingleOnSubscribe<MutableHttpResponse>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse> emitter) throws Exception {
                String cookie
                if (login.get('username') == '' || login.get('password') == '' || !login.containsKey('tmn')) {
                    emitter.onError(new Exception('Invalid login access'))
                } else {
                    //Read the URL of the tenant.
                    checkTmnUrl(configuration.getTenant().get(login.get('tmn')) as String)
                    if (login.get('remember') != null) {
                        cookie = Base64.getEncoder()
                                        .encodeToString((login.get('username') as String).getBytes()) + ':' +
                                Base64.getEncoder()
                                        .encodeToString((login.get('password') as String).getBytes())
                        emitter.onSuccess(MutableHttpResponse.permanentRedirect(new URI('/login'))
                                .cookie(new NettyCookie('credential', cookie)))
                    } else {
                        emitter.onSuccess(MutableHttpResponse.permanentRedirect(new URI('/login')))
                    }
                }
            }
        }).subscribeOn(Schedulers.io())
    }
}
