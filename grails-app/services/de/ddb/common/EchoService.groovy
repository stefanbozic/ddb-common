package de.ddb.common

import org.codehaus.groovy.grails.web.util.WebUtils

class EchoService {
    def transactional = false

    def configurationService

    def echo(String input) {
        return input
    }

    def findAll() {
        def totalInstitution = 0
        def allInstitutions = [data: [:], total: totalInstitution]
        //def apiResponse = ApiConsumer.getJson(configurationService.getBackendUrl(), '/institutions')
        def apiResponse = ApiConsumer.getJson("http://metadaten-backend.deutsche-digitale-bibliothek.de:9998", '/institutions')
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
