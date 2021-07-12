package com.cpipdm.sap.service

import com.cpipdm.sap.client.Client
import com.cpipdm.sap.configuration.Configuration
import com.cpipdm.sap.model.Dashboard
import com.cpipdm.sap.model.Error
import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder
import io.micronaut.core.beans.BeanIntrospection
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.http.server.types.files.SystemFile
import io.micronaut.http.sse.Event
import io.micronaut.session.Session
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleObserver
import io.reactivex.SingleOnSubscribe
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Pdm {
    Client client
    Configuration configuration
    Utility utility
    Error error
    ArrayList<LinkedHashMap> completeStatus = new ArrayList<LinkedHashMap>()
    boolean taskCompleted = false
    int progressDegree = 0

    @Inject
    Pdm(Client client, Configuration configuration, Utility utility, Error error) {
        this.client = client
        this.configuration = configuration
        this.utility = utility
        this.error = error
    }

    void startTask(String entityName, Session session) {
        this.completeStatus = new ArrayList<HashMap>() //Initialize.
        this.taskCompleted = false //Initialize.
        this.progressDegree = 0 //Initialize.

        Flowable.<HashMap> create(new FlowableOnSubscribe<HashMap>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<HashMap> emitter) throws Exception {
                Dashboard dashboard = readModel(session)
                int totalRecord = dashboard.getTable().size()

                dashboard.getTable().each {
                    LinkedHashMap singleStatus = new LinkedHashMap()
                    MutableHttpResponse<Map> mapMutableHttpResponse
                    try {
                        it.remove('guid') //Remove guid.
                        mapMutableHttpResponse = create(entityName, session, it, true).blockingGet()
                        singleStatus.put('Pid', it.get('Pid'))
                        singleStatus.put('Id', it.get('Id'))
                        singleStatus.put('Status', mapMutableHttpResponse.getStatus().getCode())
                        completeStatus.add(singleStatus)
                        progressDegree = (completeStatus.size() / totalRecord) * 360 //Calculate the progress.
                    }
                    catch (Exception exception) {
                        singleStatus.put('Pid', it.get('Pid'))
                        singleStatus.put('Id', it.get('Id'))
                        singleStatus.put('Status', exception.getMessage())
                        completeStatus.add(singleStatus)
                        progressDegree = (completeStatus.size() / totalRecord) * 360 //Calculate the progress.
                    }
                }
                emitter.onComplete()
            }
        }, BackpressureStrategy.ERROR).subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<HashMap>() {
                    @Override
                    void onSubscribe(Subscription s) {
                        //Start the task. This is required because this task is not started by the framework.
                        s.request(1)
                    }

                    @Override
                    void onNext(HashMap hashMap) {

                    }

                    @Override
                    void onError(Throwable t) {

                    }

                    @Override
                    void onComplete() {
                        taskCompleted = true
                    }
                })
    }

    Single<MutableHttpResponse<Map>> getPartner(Session session) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                String hostName = readModel(session).getTmn()
                client.getToken('fetch',
                        hostName,
                        'Partners')
                        .subscribeOn(Schedulers.io())
                        .subscribe(new SingleObserver<HttpResponse>() {
                            @Override
                            void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            void onSuccess(@NonNull HttpResponse httpResponse) {
                                client.getToken(httpResponse.header('x-csrf-token'),
                                        hostName,
                                        'Partners')
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(new SingleObserver<HttpResponse>() {
                                            @Override
                                            void onSubscribe(@NonNull Disposable d) {

                                            }

                                            @Override
                                            void onSuccess(@NonNull HttpResponse httpResponse_) {
                                                LinkedHashMap partnerList
                                                HashMap<String, Boolean> partnerResultList = new HashMap<String, Boolean>()
                                                ArrayList<HashMap> result = (((httpResponse_.getBody(Map.class) as Optional<Map>)
                                                        .get()
                                                        .get('d') as Map)
                                                        .get('results') as ArrayList)
                                                partnerList = readModel(session).getFilter() //Existing partner list.
                                                result.each {
                                                    HashMap it ->
                                                        if (partnerList != null) {
                                                            partnerResultList.put(it.get('Pid') as String,
                                                                    partnerList.containsKey(it.get('Pid') as String))
                                                        } else {
                                                            partnerResultList.put(it.get('Pid') as String, false)
                                                        }
                                                }
                                                emitter.onSuccess(MutableHttpResponse.<Map> ok(['table': partnerResultList]))
                                            }

                                            @Override
                                            void onError(@NonNull Throwable e) {
                                                emitter.onError(e)
                                            }
                                        })
                            }

                            @Override
                            void onError(@NonNull Throwable e) {
                                emitter.onError(e)
                            }
                        })
            }
        }).subscribeOn(Schedulers.io())
    }

    Single<MutableHttpResponse<Map>> query(String entityName, Session session) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                String hostName = readModel(session).getTmn()
                if (entityName == null || entityName == "") {
                    emitter.onError(new Exception('Entity name is missing'))
                } else {
                    client.getToken('fetch',
                            hostName,
                            entityName)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new SingleObserver<HttpResponse>() {
                                @Override
                                void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                void onSuccess(@NonNull HttpResponse httpResponse) {
                                    client.getToken(httpResponse.header('x-csrf-token'),
                                            hostName,
                                            entityName)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(new SingleObserver<HttpResponse>() {
                                                @Override
                                                void onSubscribe(@NonNull Disposable d) {

                                                }

                                                @Override
                                                void onSuccess(@NonNull HttpResponse httpResponse_) {
                                                    LinkedHashMap filter
                                                    ArrayList<LinkedHashMap> result_ = new ArrayList<LinkedHashMap>()
                                                    ArrayList<LinkedHashMap> result = (((httpResponse_.getBody(Map.class) as Optional<Map>)
                                                            .get()
                                                            .get('d') as Map)
                                                            .get('results') as ArrayList)
                                                    /**
                                                     * Remove the __metadata. Add guid.
                                                     */
                                                    result.each {
                                                        LinkedHashMap it ->
                                                            it.remove('__metadata')
                                                            it.put('guid', UUID.randomUUID().toString())
                                                    }
                                                    /**
                                                     * Apply filter (if available).
                                                     */
                                                    filter = readModel(session).getFilter()
                                                    if (filter != null) {
                                                        filter.each {
                                                            result.each {
                                                                LinkedHashMap map ->
                                                                    if (map.get('Pid') == it.getKey()) {
                                                                        result_.add(map)
                                                                    }
                                                            }
                                                        }
                                                        result = result_
                                                    }
                                                    updateModel(session, result)
                                                    /**
                                                     * Special handling for BinaryParameters.
                                                     */
                                                    if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : result,
                                                                                                        'readOnly': configuration.getReadOnly(),
                                                                                                        'special' : true,
                                                                                                        'message' : "${result.size()} records found"])
                                                                .status(HttpStatus.OK))
                                                    } else {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : result,
                                                                                                        'readOnly': configuration.getReadOnly(),
                                                                                                        'message' : "${result.size()} records found"])
                                                                .status(HttpStatus.OK))
                                                    }
                                                }

                                                @Override
                                                void onError(@NonNull Throwable e) {
                                                    emitter.onError(e)
                                                }
                                            })
                                }

                                @Override
                                void onError(@NonNull Throwable e) {
                                    emitter.onError(e)
                                }
                            })
                }
            }
        }).subscribeOn(Schedulers.io())
    }

    Single<MutableHttpResponse<Map>> update(String entityName,
                                            Session session,
                                            LinkedHashMap body) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                String hostName = readModel(session).getTmn()
                if (entityName == null || entityName == "") {
                    emitter.onError(new Exception('Entity name is missing'))
                } else {
                    client.getToken('fetch',
                            hostName,
                            entityName)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new SingleObserver<HttpResponse>() {
                                @Override
                                void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                void onSuccess(@NonNull HttpResponse httpResponse) {
                                    String primaryKey = constructPrimaryKey(entityName, body)
                                    String cookieValue = httpResponse.getCookie('__Host-csrf-client-id')
                                            .get()
                                            .getValue()
                                    body = consolidate(session, entityName, body)
                                    client.update(httpResponse.header('x-csrf-token'),
                                            hostName,
                                            entityName,
                                            primaryKey,
                                            cookieValue,
                                            body)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(new SingleObserver<HttpResponse>() {
                                                @Override
                                                void onSubscribe(@NonNull Disposable d) {

                                                }

                                                @Override
                                                void onSuccess(@NonNull HttpResponse httpResponse_) {
                                                    /**
                                                     * Special handling for BinaryParameters.
                                                     */
                                                    if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : readModel(session).getTable(),
                                                                                                        'readOnly': configuration.getReadOnly(),
                                                                                                        'special' : true,
                                                                                                        'message' : "Update successful on ${new Date()}"])
                                                                .status(HttpStatus.OK))
                                                    } else {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : readModel(session).getTable(),
                                                                                                        'readOnly': configuration.getReadOnly(),
                                                                                                        'message' : "Update successful on ${new Date()}"])
                                                                .status(HttpStatus.OK))

                                                    }
                                                }

                                                @Override
                                                void onError(@NonNull Throwable e) {
                                                    emitter.onError(e)
                                                }
                                            })
                                }

                                @Override
                                void onError(@NonNull Throwable e) {
                                    emitter.onError(e)
                                }
                            })
                }
            }
        }).subscribeOn(Schedulers.io())
    }

    Single<MutableHttpResponse<Map>> delete(String entityName,
                                            Session session,
                                            Map body) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                String hostName = readModel(session).getTmn()
                if (entityName == null || entityName == "") {
                    emitter.onError(new Exception('Entity name is missing'))
                } else {
                    client.getToken('fetch',
                            hostName,
                            entityName)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new SingleObserver<HttpResponse>() {
                                @Override
                                void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                void onSuccess(@NonNull HttpResponse httpResponse) {
                                    String primaryKey = constructPrimaryKey(entityName, body)
                                    String cookieValue = httpResponse.getCookie('__Host-csrf-client-id')
                                            .get()
                                            .getValue()
                                    client.delete(httpResponse.header('x-csrf-token'),
                                            hostName,
                                            entityName,
                                            primaryKey,
                                            cookieValue)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(new SingleObserver<HttpResponse>() {
                                                @Override
                                                void onSubscribe(@NonNull Disposable d) {

                                                }

                                                @Override
                                                void onSuccess(@NonNull HttpResponse httpResponse_) {
                                                    Dashboard dashboard
                                                    Integer i = 0
                                                    /**
                                                     * Update the page model with the new data.
                                                     */
                                                    dashboard = readModel(session)
                                                    dashboard.getTable().each {
                                                        if ((it.get('guid') == body.get('guid'))) {
                                                            i = dashboard.getTable().indexOf(it)
                                                        }
                                                    }
                                                    dashboard.getTable().remove(i)
                                                    updateModel(session, dashboard.getTable()) //Update model.
                                                    /**
                                                     * Special handling for BinaryParameters.
                                                     */
                                                    if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : dashboard.getTable(),
                                                                                                        'readOnly': configuration.getReadOnly(),
                                                                                                        'special' : true,
                                                                                                        'message' : "Delete successful on ${new Date()}"])
                                                                .status(HttpStatus.OK))
                                                    } else {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : dashboard.getTable(),
                                                                                                        'readOnly': configuration.getReadOnly(),
                                                                                                        'message' : "Delete successful on ${new Date()}"])
                                                                .status(HttpStatus.OK))

                                                    }
                                                }

                                                @Override
                                                void onError(@NonNull Throwable e) {
                                                    emitter.onError(e)
                                                }
                                            })
                                }

                                @Override
                                void onError(@NonNull Throwable e) {
                                    emitter.onError(e)
                                }
                            })
                }
            }
        }).subscribeOn(Schedulers.io())
    }

    Single<MutableHttpResponse<Map>> newRow(String entityName, Session session) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                LinkedHashMap newRow = new LinkedHashMap()
                ArrayList<String> fieldList = new ArrayList<String>()
                Dashboard dashboard

                if (entityName == null || entityName == "") {
                    emitter.onError(new Exception('Entity name is missing'))
                } else {
                    fieldList = configuration.fieldList.getProperty(entityName.toLowerCase()) as ArrayList
                    fieldList.each {
                        newRow.put(it, '')
                    }
                    newRow.put('guid', UUID.randomUUID().toString())
                    dashboard = readModel(session)
                    if (dashboard.getTable() == null || dashboard.getTable().size() == 0) {
                        dashboard.setTable(new ArrayList<LinkedHashMap>())
                    } else {
                        /**
                         * Check for key-set consistency.
                         */
                        if (dashboard.getTable().get(0).keySet().size() != fieldList.size()) {
                            emitter.onError(new Exception('You cannot add new rows of different entities'))
                            return
                        }
                    }
                    dashboard.getTable().add(newRow)
                    updateModel(session, dashboard.getTable()) //Update model.
                    /**
                     * Special handling for BinaryParameters.
                     */
                    if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : dashboard.getTable(),
                                                                        'readOnly': configuration.getReadOnly(),
                                                                        'special' : true,
                                                                        'message' : "New row created for ${entityName}"])
                                .status(HttpStatus.OK))
                    } else {
                        emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : dashboard.getTable(),
                                                                        'readOnly': configuration.getReadOnly(),
                                                                        'message' : "New row created for ${entityName}"])
                                .status(HttpStatus.OK))
                    }
                }
            }
        }).subscribeOn(Schedulers.io())
    }

    Single<MutableHttpResponse<Map>> create(String entityName,
                                            Session session,
                                            LinkedHashMap body,
                                            boolean cutOver = false) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                String hostName = readModel(session).getTmn()
                if (entityName == null || entityName == "") {
                    emitter.onError(new Exception('Entity name is missing'))
                } else {
                    client.getToken('fetch',
                            hostName,
                            entityName)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new SingleObserver<HttpResponse>() {
                                @Override
                                void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                void onSuccess(@NonNull HttpResponse httpResponse) {
                                    String cookieValue = httpResponse.getCookie('__Host-csrf-client-id')
                                            .get()
                                            .getValue()
                                    body = consolidate(session, entityName, body, cutOver)
                                    client.create(httpResponse.header('x-csrf-token'),
                                            hostName,
                                            entityName,
                                            cookieValue,
                                            body)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(new SingleObserver<HttpResponse>() {
                                                @Override
                                                void onSubscribe(@NonNull Disposable d) {

                                                }

                                                @Override
                                                void onSuccess(@NonNull HttpResponse httpResponse_) {
                                                    /**
                                                     * Add new guid.
                                                     */
                                                    if (cutOver) {
                                                        emitter.onSuccess(MutableHttpResponse.<Map> ok())
                                                    } else {
                                                        /**
                                                         * Special handling for BinaryParameters.
                                                         */
                                                        if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                                                            emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : readModel(session).getTable(),
                                                                                                            'readOnly': configuration.getReadOnly(),
                                                                                                            'special' : true,
                                                                                                            'message' : "Create successful on ${new Date()}"])
                                                                    .status(HttpStatus.OK))
                                                        } else {
                                                            emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : readModel(session).getTable(),
                                                                                                            'readOnly': configuration.getReadOnly(),
                                                                                                            'message' : "Create successful on ${new Date()}"])
                                                                    .status(HttpStatus.OK))

                                                        }
                                                    }
                                                }

                                                @Override
                                                void onError(@NonNull Throwable e) {
                                                    emitter.onError(e)
                                                }
                                            })
                                }

                                @Override
                                void onError(@NonNull Throwable e) {
                                    emitter.onError(e)
                                }
                            })
                }
            }
        }).subscribeOn(Schedulers.io())
    }

    String constructPrimaryKey(String entityName, Map body) {
        ArrayList<String> primaryKey = new ArrayList<String>()
        String primaryKey_ = ''

        /**
         * Get the primary key of this entity.
         */
        this.configuration.getKeyField().each {
            if (it.containsKey(entityName)) {
                primaryKey = it.get(entityName) as ArrayList<String>
            }
        }
        body.each {
            if (primaryKey.contains(it.getKey())) {
                primaryKey_ = primaryKey_ + "," + it.getKey() + '=\'' + it.getValue() + '\''
            }
        }
        return primaryKey_.substring(1)
    }

    Map consolidate(Session session, String entityName, Map body, boolean cutOver = false) {
        /**
         * Local variable declaration.
         */
        ArrayList<LinkedHashMap> dashboard = new ArrayList<LinkedHashMap>()
        int i = 0

        dashboard = readModel(session).getTable()
        dashboard.each {
            if (it.get('guid') == body.get('guid')) {
                i = dashboard.indexOf(it) //Index to be removed.
                /**
                 * For binary parameters always update the body with the content type and the value.
                 */
                if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                    body.put('Value', it.get('Value'))
                    body.put('ContentType', it.get('ContentType'))
                }
            }
        }
        /**
         * Consolidate the model only if it is not a cut-over activity.
         */
        if (!cutOver) {
            dashboard.remove(i) //Remove the changed row.
            dashboard.add(body) //Update the model with the newest change.
            this.updateModel(session, dashboard) //Update the model in the session.
        }
        body.remove('guid') //Remove the guid before posting in API.
        return body
    }

    Single<MutableHttpResponse<Map>> upload(String entityName,
                                            Session session,
                                            StreamingFileUpload file,
                                            Boolean header) {
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), '.csv')
        return Single.<Boolean> fromPublisher(file.transferTo(tempFile))
                .subscribeOn(Schedulers.io())
                .map(new Function<Boolean, MutableHttpResponse<Map>>() {
                    @Override
                    MutableHttpResponse<Map> apply(@NonNull Boolean aBoolean) throws Exception {
                        ArrayList<LinkedHashMap> table = new ArrayList<LinkedHashMap>()
                        switch (aBoolean) {
                            case true:
                                /**
                                 * Begin file validations.
                                 */
                                tempFile.withReader {
                                    it.splitEachLine('\t', {
                                        List<String> row ->
                                            LinkedHashMap row_ = new LinkedHashMap()
                                            Integer i = 0
                                            row.each {
                                                String field ->
                                                    row_.put((configuration.fieldList
                                                            .getProperty(entityName.toLowerCase()) as ArrayList).get(i) as String, field)
                                                    i++
                                            }
                                            row_.put('guid', UUID.randomUUID().toString())
                                            table.add(row_)
                                    })
                                }
                                // Remove first line.
                                if (header) {
                                    table.remove(0)
                                }
                                if (table.get(0).keySet().size() != (configuration.fieldList
                                        .getProperty(entityName.toLowerCase()) as ArrayList).size()) {
                                    return error.raiseErrorMessage('The file uploaded file and the chosen entity does not match')
                                            .blockingGet()
                                }
                                updateModel(session, table)
                                /**
                                 * Special handling for BinaryParameters.
                                 */
                                if (entityName.toUpperCase() == 'BINARYPARAMETERS') {
                                    return MutableHttpResponse.<Map> ok(['table'   : table,
                                                                         'inline'  : true,
                                                                         'special' : true,
                                                                         'readOnly': configuration.getReadOnly(),
                                                                         'message' : "${table.size()} record(s) uploaded"])
                                } else {
                                    return MutableHttpResponse.<Map> ok(['table'   : table,
                                                                         'inline'  : true,
                                                                         'readOnly': configuration.getReadOnly(),
                                                                         'message' : "${table.size()} record(s) uploaded"])
                                }
                                break
                            default:
                                return error.raiseErrorMessage('There was some issue with file upload')
                                        .blockingGet()
                        }
                    }
                })
    }

    SystemFile download(String entityName, Session session) {
        File file
        SystemFile systemFile
        Dashboard dashboard

        dashboard = this.readModel(session)
        file = File.createTempFile("${entityName} (TAB separated)", '.csv')
        file.withWriter {
            BufferedWriter bufferedWriter ->
                String line = ''
                /**
                 * File header.
                 */
                this.configuration
                        .getFieldList()
                        .getProperty(entityName.toLowerCase()).each {
                    String it ->
                        if (it != 'guid') {
                            line = line + it + '\t'
                        }
                }
                bufferedWriter.writeLine(line)
                dashboard.getTable().each {
                    LinkedHashMap linkedHashMap ->
                        line = ''
                        linkedHashMap.remove('guid') //remove the guid.
                        linkedHashMap.each {
                            line = line + it.getValue() + '\t'
                        }
                        bufferedWriter.writeLine(line)
                }
        }
        return new SystemFile(file).attach("${entityName} (TAB separated).csv")
    }

    Flowable<Event<String>> startBatch() {
        return Flowable.<Event<String>> create(new FlowableOnSubscribe<Event<String>>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Event<String>> emitter) throws Exception {
                JsonBuilder jsonBuilder = new JsonBuilder()
                String htmlStatus = buildHTML().replace('\n', '') //Replace all new lines.
                String progressStatus = buildProgress().replace('\n', '') //Replace all new lines.
                jsonBuilder."result" {
                    "status"(taskCompleted)
                    "payload"(htmlStatus)
                    "progress"(progressStatus)
                }
                emitter.onNext(Event.of(jsonBuilder.toString()))
                emitter.onComplete()
            }
        }, BackpressureStrategy.BUFFER).subscribeOn(Schedulers.io())
    }

    String buildHTML() {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(stringWriter)
        Integer row = 0
        html."table"("class": "batch_table", "cellspacing": "0") {
            "thead" {
                "tr" {
                    "th"("Partner ID")
                    "th"("Id")
                    "th"("HTTPStatus Code")
                }
            }
            "tbody" {
                this.completeStatus.each {
                    LinkedHashMap map ->
                        "tr"("class": "${row % 2 == 0 ? 'even' : 'odd'}") {
                            map.each {
                                "td"(it.value)
                            }
                        }
                        row++
                }
            }
        }
        return stringWriter.toString()
    }

    String buildProgress() {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(stringWriter)
        html."div"('class': 'piechart',
                'style': "background-image: conic-gradient(brown ${this.progressDegree}deg, orange 0);") {
            "h4"('class': 'progress', "${Math.round((this.progressDegree / 360) * 100)} %")
        }
        return stringWriter.toString()
    }

    Single<MutableHttpResponse<Map>> showBinary(Session session, Map body) {
        /**
         * Read the partner-id and id.
         */
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                HashMap<String, Boolean> contentTypeChecked = new HashMap<String, Boolean>()
                LinkedHashMap record = new LinkedHashMap()
                String value, contentType
                Dashboard dashboard

                dashboard = readModel(session)
                dashboard.getTable().each {
                    if (it.get('guid') == body.get('guid')) {
                        record = it
                        contentType = it.get('ContentType') as String
                    }
                }
                if (record.get('Value') != '') {
                    value = new String(Base64.getDecoder().decode(record.get('Value') as String))
                }
                record.put('Value', value)
                /**
                 * Change the content-type to accept all the configured content types.
                 */
                configuration.fieldList.getFiletype().each {
                    String it ->
                        contentTypeChecked.put(it, it == contentType)
                }
                record.put('ContentType', contentTypeChecked)
                emitter.onSuccess(MutableHttpResponse.<Map> ok(record))
            }
        }).subscribeOn(Schedulers.io())
    }

    Single<MutableHttpResponse<Map>> uploadBinary(Session session, Map body) {
        return Single.<MutableHttpResponse<Map>> create(new SingleOnSubscribe<MutableHttpResponse<Map>>() {
            @Override
            void subscribe(@NonNull SingleEmitter<MutableHttpResponse<Map>> emitter) throws Exception {
                Dashboard dashboard
                int i = 0

                dashboard = readModel(session)
                dashboard.getTable().each {
                    if (it.get('guid') == body.get('guid')) {
                        i = dashboard.getTable().indexOf(it) //Get the index of the modified row.
                    }
                }
                /**
                 * Update the content-type and value from the popup.
                 */
                dashboard.getTable().get(i).put('Value', Base64.getEncoder()
                        .encodeToString((body.get('Value') as String).getBytes()))
                dashboard.getTable().get(i).put('ContentType', body.get('ContentType') as String)
                updateModel(session, dashboard.getTable())
                emitter.onSuccess(MutableHttpResponse.<Map> ok(['table'   : dashboard.getTable(),
                                                                'readOnly': configuration.getReadOnly(),
                                                                'special' : true]))
            }
        }).subscribeOn(Schedulers.io())
    }

    Boolean isOld(Session session, Map body, String entityName) {
        ArrayList<String> primaryKey = new ArrayList<String>()
        Dashboard dashboard = readModel(session)
        Boolean isOld = false, isDifferent = false

        /**
         * Get the primary key of this entity. However, for Alternative Partners, the form submitted will not have
         * the hex fields populated, so only to determine if a row exists or not we cannot use the PK but we need to
         * check using the non-hex fields.
         */
        if (entityName.toUpperCase() == 'ALTERNATIVEPARTNERS') {
            primaryKey = ['Agency', 'Scheme', 'Id']
        } else {
            this.configuration.getKeyField().each {
                if (it.containsKey(entityName)) {
                    primaryKey = it.get(entityName) as ArrayList<String>
                }
            }
        }
        dashboard.getTable().each {
            LinkedHashMap row ->
                isDifferent = false
                row.each {
                    if (primaryKey.contains(it.getKey())) {
                        if (it.getValue() != body.get(it.getKey())) {
                            isDifferent = true
                        }
                    }
                }
                if (!isDifferent) {
                    isOld = true
                }
        }
        return isOld
    }

    void initializeModelToSession(Session session, Map<String, String> login) throws Exception {
        Dashboard dashboard

        try {
            dashboard = BeanIntrospection.getIntrospection(Dashboard).instantiate()
            dashboard.setUsername(login.get('username')) //Save the username.
            dashboard.setPassword(login.get('password')) //Save the password.
            dashboard.setSystemId(login.get('tmn')) //Save the system id.
            dashboard.setTmn(configuration.getTenant().get(login.get('tmn')) as String) //Save the tmn.
            if (session.get('model').isPresent()) {
                dashboard.setFilter(this.readModel(session).getFilter()) //Save the filter criteria.
            }
            session.put('model', dashboard)
        }
        catch (Exception exception) {
            throw exception
        }
    }

    Dashboard readModel(Session session) throws Exception {
        Dashboard dashboard

        if (session.get('model').isPresent()) {
            dashboard = session.get('model', Dashboard).get()
        } else {
            throw new Exception('Session invalidated. Please re-login')
        }
        return dashboard
    }

    void updateModel(Session session, ArrayList<LinkedHashMap> table) throws Exception {
        Dashboard dashboard

        dashboard = this.readModel(session)
        dashboard.setTable(table) //Update the model.
        session.put('model', dashboard) //Update the model.
    }

    void updatePartnerList(Session session, Map partnerList) throws Exception {
        Dashboard dashboard

        dashboard = this.readModel(session)
        dashboard.setFilter(partnerList) //Update the model.
        session.put('model', dashboard) //Update the model.
    }
}
