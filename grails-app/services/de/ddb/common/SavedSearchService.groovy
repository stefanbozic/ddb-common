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

import grails.converters.JSON
import groovy.json.*
import net.sf.json.JSONNull

/**
 * @author chh
 *
 */
class SavedSearchService {
    static final def DEFAULT_SIZE = 9999

    def configurationService
    def transactional = false

    def saveSearch(userId, queryString, title = null, description = null) {
        log.info "saveSearch()"
        def postBody = [
            user: userId,
            queryString: queryString,
            title: title,
            description: description,
            createdAt: new Date().getTime()
        ]

        ApiResponse apiResponse = ApiConsumer.postJson(configurationService.getElasticSearchUrl(), "/ddb/savedSearch", false, postBody as JSON)

        if(apiResponse.isOk()){
            def response = apiResponse.getResponse()
            def savedSearchId = response._id
            log.info "Saved Search with the ID ${savedSearchId} is created."
            refresh()

            return savedSearchId
        }
    }

    def deleteSavedSearchesByUserId(userId) {
        def userSearches = []

        findSavedSearchByUserId(userId).each{ it ->
            userSearches.add(it['id'])
        }

        deleteSavedSearch(userSearches)
    }


    def findSavedSearchByUserId(userId) {
        log.info "findSavedSearchByUserId(): find saved searches for the user (${userId})"
        return findSavedSearch(["q": "user:\"${userId}\"".encodeAsURL(), "size": DEFAULT_SIZE])
    }

    def findSavedSearchByQueryString(userId, queryString) {
        log.info "findSavedSearchByQueryString(): find saved searches for the user ${userId} and query ${queryString}"
        return findSavedSearch(["q": "user:\"${userId}\" AND queryString:\"${queryString}\"".encodeAsURL(), "size": DEFAULT_SIZE])
    }

    private def findSavedSearch(def query) {
        log.info "findSavedSearch()"
        ApiResponse apiResponse = ApiConsumer.getJson(configurationService.getElasticSearchUrl(), "/ddb/savedSearch/_search", false, query, [:], true)

        if(apiResponse.isOk()){
            def response = apiResponse.getResponse()
            def all = []
            def resultList = response.hits.hits

            resultList.each { it ->
                def savedSearch = [:]

                savedSearch['id'] = it._id
                savedSearch['user'] = it._source.user
                savedSearch['title'] = it._source.title
                savedSearch['description'] = it._source.description
                savedSearch['queryString'] = it._source.queryString
                savedSearch['createdAt'] = it._source.createdAt

                all.add(savedSearch)

                log.info "it: ${it}"
                log.info "Saved Search ID: ${it._id}"
                log.info "user: ${it._source.user}"
                log.info "title: ${it._source.title}"
                log.info "description: ${it._source.description}"
                log.info "query string: ${it._source.queryString}"
            }
            return all
        }
    }

    def deleteSavedSearch(savedSearchIdList) {
        log.info "deleteSavedSearch()"
        def postBody = ''
        savedSearchIdList.each { id ->
            postBody = postBody + '{ "delete" : { "_index" : "ddb", "_type" : "savedSearch", "_id" : "' + id + '" } }\n'
        }
        ApiResponse apiResponse = ApiConsumer.postJson(configurationService.getElasticSearchUrl(), "/ddb/savedSearch/_bulk", false, postBody)

        if(apiResponse.isOk()){
            refresh()
            return true
        }
    }

    // TODO: move to a util class.
    private refresh() {
        log.info "refresh()"
        ApiResponse apiResponse = ApiConsumer.postJson(configurationService.getElasticSearchUrl(), "/ddb/_refresh", false, "")

        if(apiResponse.isOk()){
            def response = apiResponse.getResponse()
            log.info "Response: ${response}"
            log.info "finished refreshing index ddb."
        }
    }

    def getSearch(id) {
        log.info "getSearch()"
        def ApiResponse apiResponse = ApiConsumer.getJson(configurationService.getElasticSearchUrl(), "/ddb/savedSearch/" +
                id, false)
        if(apiResponse.isOk()) {
            def response = apiResponse.getResponse()
            log.info "Response: ${response}"
            return response
        }
    }

    def updateSearch(id, title = null) {
        log.info "updateSearch()"
        def oldValue = getSearch(id)
        def putBody = [
            user: oldValue._source.user,
            queryString: oldValue._source.queryString.class != JSONNull ? oldValue._source.queryString : null,
            title: title,
            description: oldValue._source.description.class != JSONNull ? oldValue._source.description : null,
            createdAt: oldValue._source.createdAt.class != JSONNull ? oldValue._source.createdAt : null
        ]
        def ApiResponse apiResponse = ApiConsumer.putJson(configurationService.getElasticSearchUrl(), "/ddb/savedSearch/" +
                id, false, putBody as JSON)
        if(apiResponse.isOk()) {
            def response = apiResponse.getResponse()
            def savedSearchId = response._id
            log.info "Saved Search with the ID ${savedSearchId} is updated."
            refresh()
            return savedSearchId
        }
    }

    int getSavedSearchesCount() {
        int count = -1

        ApiResponse apiResponse = ApiConsumer.getJson(configurationService.getElasticSearchUrl(), "/ddb/savedSearch/_search", false)

        if(apiResponse.isOk()){
            def response = apiResponse.getResponse()
            count = response.hits.total
        }

        return count
    }

}
