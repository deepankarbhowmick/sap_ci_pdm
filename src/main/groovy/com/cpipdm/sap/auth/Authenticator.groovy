package com.cpipdm.sap.auth

import com.cpipdm.sap.client.Client
import com.cpipdm.sap.configuration.Configuration
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.BasicAuth
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.authentication.UserDetails
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.SingleObserver
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Authenticator implements AuthenticationProvider {
    Client client
    Configuration configuration

    @Inject
    Authenticator(Client client, Configuration configuration) {
        this.client = client
        this.configuration = configuration
    }

    Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest,
                                                   AuthenticationRequest<?, ?> authenticationRequest) {
        return Flowable.<UserDetails> create(new FlowableOnSubscribe<UserDetails>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<UserDetails> emitter) throws Exception {
                /**
                 * Local variable declaration.
                 */
                UserDetails userDetails
                Map login
                String hostname
                
                login = httpRequest.getBody(Map).get()
                hostname = configuration.getTenant().get(login.get('tmn'))
                client.getMetaData(hostname, new BasicAuth(login.get('username') as String, login.get('password') as String))
                        .subscribeOn(Schedulers.io())
                        .subscribe(new SingleObserver<HttpResponse>() {
                            @Override
                            void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            void onSuccess(@NonNull HttpResponse httpResponse) {
                                userDetails = new UserDetails(authenticationRequest.getIdentity() as String, [])
                                userDetails.setAttributes(['login': login])
                                emitter.onNext(userDetails)
                            }

                            @Override
                            void onError(@NonNull Throwable e) {
                                emitter.onError(new AuthorizationException(new Authentication() {
                                    @Override
                                    Map<String, Object> getAttributes() {
                                        return null
                                    }

                                    @Override
                                    String getName() {
                                        return authenticationRequest.getIdentity()
                                    }
                                }))
                            }
                        })
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
    }
}
