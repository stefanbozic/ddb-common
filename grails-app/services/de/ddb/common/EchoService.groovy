/*
 * Copyright (C) 2013 FIZ Karlsruhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.common

import org.codehaus.groovy.grails.web.util.WebUtils

class EchoService {
    def transactional = false

    def configurationService

    TestService testService

    def echo(String input) {
        return input
    }

    def callTestService() {
        return testService.test()
    }

    def findAll() {
        def totalInstitution = 0
        def allInstitutions = [data: [:], total: totalInstitution]
        println "configurationService::::::::::::::::::::: " +configurationService
        def apiResponse = ApiConsumer.getJson(configurationService.getBackendUrl(), '/institutions')
        //def apiResponse = ApiConsumer.getJson("http://metadaten-backend.deutsche-digitale-bibliothek.de:9998", '/institutions')
        if (apiResponse.isOk()) {
            def institutionList = apiResponse.getResponse()

            allInstitutions.data = institutionList
            allInstitutions.total = institutionList.size()
        }
        else {
            log.error "findAll: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }

        return allInstitutions
    }
}
