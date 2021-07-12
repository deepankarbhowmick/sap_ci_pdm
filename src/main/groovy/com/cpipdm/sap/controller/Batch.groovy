package com.cpipdm.sap.controller

import com.cpipdm.sap.service.Pdm
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.sse.Event
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.View
import io.reactivex.Flowable
import javax.inject.Inject

@Controller('/batch')
class Batch {

    Pdm pdm

    @Inject
    Batch(Pdm pdm) {
        this.pdm = pdm
    }

    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    @View('batch')
    MutableHttpResponse<Map> startBatch() {
        return HttpResponse.<Map> ok([:])
    }

    @Get(value = '/sse', produces = MediaType.TEXT_EVENT_STREAM)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Flowable<Event<String>> sse() {
        return this.pdm.startBatch()
    }

    @Get('/back')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    MutableHttpResponse backToDashboard() {
        return MutableHttpResponse.redirect(new URI('/dashboard'))
    }
}
