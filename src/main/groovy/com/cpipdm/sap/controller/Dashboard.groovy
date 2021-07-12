package com.cpipdm.sap.controller

import com.cpipdm.sap.service.Pdm
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Part
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthenticationUserDetailsAdapter
import io.micronaut.security.rules.SecurityRule
import io.micronaut.session.Session
import io.micronaut.views.View
import io.micronaut.views.ViewsRenderer
import io.reactivex.Single
import javax.inject.Inject
import java.security.Principal

@Controller('/')
class Dashboard {
    Pdm pdm
    ViewsRenderer viewsRenderer
    com.cpipdm.sap.model.Error error

    @Inject
    Dashboard(Pdm pdm, ViewsRenderer viewsRenderer, com.cpipdm.sap.model.Error error) {
        this.pdm = pdm
        this.viewsRenderer = viewsRenderer
        this.error = error
    }

    @Get(value = '/dashboard', produces = MediaType.TEXT_HTML)
    @View('dashboard')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    MutableHttpResponse<Map> index(Principal principal, Session session) {
        Map login

        /**
         * Save the login specific details in the session.
         */
        login = (principal as AuthenticationUserDetailsAdapter).getAttributes()
                .get('login') as Map
        this.pdm.initializeModelToSession(session, login)
        return MutableHttpResponse.<Map> ok(['userName': this.pdm.readModel(session).getUsername(),
                                             'system'  : this.pdm.readModel(session).getSystemId()])
    }

    @Get(value = '/query')
    @View('components/table')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Single<MutableHttpResponse<Map>> query(@QueryValue('entity') String entityName, Session session) {
        return this.pdm.query(entityName, session)
    }

    @Post(value = '/upsert', consumes = MediaType.APPLICATION_JSON)
    @View('components/table')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Single<MutableHttpResponse<Map>> create(@Body Map mapBody, Session session) {
        String entityName = mapBody.get('entity') as String
        mapBody.remove('entity')
        /**
         * Check if create is required or update.
         */
        if (this.pdm.isOld(session, mapBody, entityName)) {
            return this.pdm.update(entityName, session, mapBody as LinkedHashMap)
        } else {
            return this.pdm.create(entityName, session, mapBody as LinkedHashMap)
        }
    }

    @Get(value = '/newRow')
    @View('components/table')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Single<MutableHttpResponse<Map>> newRow(@QueryValue('entity') String entityName, Session session) {
        return this.pdm.newRow(entityName, session)
    }

    @Post(value = '/delete', consumes = MediaType.APPLICATION_JSON)
    @View('components/table')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Single<MutableHttpResponse<Map>> delete(Session session,
                                            @Body Map mapBody) {
        String entityName = mapBody.get('entity') as String
        mapBody.remove('entity')
        return this.pdm.delete(entityName, session, mapBody)
    }

    @Get(value = '/download', produces = MediaType.APPLICATION_OCTET_STREAM)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    def download(@QueryValue('entity') String entityName, Session session) {
        if (entityName == null || entityName == '') {
            return this.error.raiseErrorMessage('Entity is not available')
        } else if (this.pdm
                .readModel(session)
                .getTable() == null ||
                this.pdm
                        .readModel(session)
                        .getTable().size == 0) {
            return this.error.raiseErrorMessage('Please query the entity first before downloading')
        }
        return this.pdm.download(entityName, session)
    }

    @Get('/filter')
    @View('components/filter')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Single<MutableHttpResponse<Map>> filterOn(Session session) {
        return this.pdm.getPartner(session)
    }

    @Post(value = '/partner', produces = MediaType.TEXT_HTML, consumes = MediaType.APPLICATION_FORM_URLENCODED)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    MutableHttpResponse partner(@Nullable @Body Map partnerList, Session session) {
        this.pdm.updatePartnerList(session, partnerList)
        HttpResponse.redirect(new URI('/dashboard'))
    }

    @Post(value = '/showBinary', consumes = MediaType.APPLICATION_JSON)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    @View('components/binary')
    Single<MutableHttpResponse<Map>> showBinary(@Body Map body, Session session) {
        return this.pdm.showBinary(session, body)
    }

    @Post(value = '/uploadBinary', consumes = MediaType.APPLICATION_JSON)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    @View('components/table')
    Single<MutableHttpResponse<Map>> uploadBinary(@Body Map body, Session session) {
        return this.pdm.uploadBinary(session, body)
    }

    @Post(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA)
    @View('dashboard')
    @Secured(SecurityRule.IS_AUTHENTICATED)
    Single<MutableHttpResponse<Map>> upload(@Nullable @Part('cutover') StreamingFileUpload file,
                                            @Nullable @Part('entity') String entityName,
                                            @Nullable @Part('header') Boolean header,
                                            Session session) {
        if (entityName == null || entityName == '' || file == null) {
            this.error.raiseErrorMessage('Either entity of the file is not available')
        } else {
            return this.pdm.upload(entityName, session, file, header)
        }
    }

    @Post(value = '/redirectBatch', consumes = MediaType.APPLICATION_FORM_URLENCODED)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    MutableHttpResponse startBatch(@Body Map body, Session session) {
        /**
         * Start validation before starting the batch task.
         */
        if (this.pdm
                .readModel(session)
                .getTable() == null ||
                this.pdm
                        .readModel(session)
                        .getTable()
                        .size() == 0 ||
                body.get('entity') == '' ||
                body.get('entity') == null) {
            return this.error
                    .raiseErrorMessage('There is nothing to start batch. Please import data first')
                    .blockingGet()
        }
        this.pdm.startTask(body.get('entity') as String, session) //Start the batch task.
        return MutableHttpResponse.redirect(new URI('/batch')) //GET to the batch controller.
    }

    @Post(value = '/customLogout', consumes = MediaType.APPLICATION_FORM_URLENCODED)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    MutableHttpResponse logout() {
        return MutableHttpResponse.permanentRedirect(new URI('/logout'))
    }

    @Post(value = '/errorHandler', consumes = MediaType.APPLICATION_FORM_URLENCODED)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    MutableHttpResponse errorHandler() {
        MutableHttpResponse.redirect(new URI('/dashboard'))
    }


    // This error handler is to handle errors which are raised by the JS call and needs to be displayed as a popup.
    @Error(exception = Exception)
    MutableHttpResponse<String> handleLocalError(HttpRequest httpRequest, Exception exception) {
        return MutableHttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage())
    }

    // This error is to handle the errors which are raised by POST call directly. They will display in view.
    @Error(status = HttpStatus.CONFLICT)
    @View('dashboard')
    MutableHttpResponse<Map> handleLocalError_(HttpRequest httpRequest, Session session) {
        return MutableHttpResponse.<Map> ok(['isError'  : true,
                                             'errorText': this.error.getMessage()])
    }
}
