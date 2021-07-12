package com.cpipdm.sap.middleware

import com.cpipdm.sap.model.Dashboard
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.Filter
import io.micronaut.http.context.ServerRequestContext
import io.micronaut.http.filter.ClientFilterChain
import io.micronaut.http.filter.HttpClientFilter
import io.micronaut.session.http.SessionForRequest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import javax.inject.Singleton

@Singleton
@Filter(patterns = '/api/v1/**')
class Client implements HttpClientFilter {

    Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        return Flowable.<Boolean> create(new FlowableOnSubscribe<Boolean>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Boolean> emitter) throws Exception {
                Dashboard dashboard
                try {
                    if (request.headers.get('Authorization') == null) {
                        dashboard = SessionForRequest.find(ServerRequestContext.currentRequest()
                                .get())
                                .get()
                                .get('model', Dashboard)
                                .get()
                        request.basicAuth(dashboard.getUsername(), dashboard.getPassword())
                    }
                    emitter.onNext(true)
                    emitter.onComplete()
                }
                catch (Exception exception) {
                    emitter.onError(exception)
                }
            }
        }, BackpressureStrategy.ERROR)
                .switchMap(new Function<Boolean, Publisher<? extends HttpResponse>>() {
                    @Override
                    Publisher<? extends HttpResponse> apply(@NonNull Boolean aBoolean) throws Exception {
                        switch (aBoolean) {
                            case true:
                                return chain.proceed(request)
                                break
                            default:
                                throw new Exception('Some issue with API access.')
                        }
                    }
                }).subscribeOn(Schedulers.io())
    }
}