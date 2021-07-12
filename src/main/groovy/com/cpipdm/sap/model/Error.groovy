package com.cpipdm.sap.model

import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.runtime.http.scope.RequestScope
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

import java.util.concurrent.Callable

@RequestScope
class Error {
    String message

    Single<MutableHttpResponse<Map>> raiseErrorMessage(String messageText) {
        Single.<MutableHttpResponse<Map>> fromCallable(new Callable<MutableHttpResponse<Map>>() {
            @Override
            MutableHttpResponse<Map> call() throws Exception {
                message = messageText
                return MutableHttpResponse.status(HttpStatus.CONFLICT)
            }
        }).subscribeOn(Schedulers.io())
    }
}
