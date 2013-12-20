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

import static groovyx.net.http.ContentType.*
import groovy.json.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.util.WebUtils

import de.ddb.common.beans.User

/**
 * Set of Methods that encapsulate REST-calls to the NewsletterService
 *
 * @author chh
 *
 */

class NewsletterService {

    def configurationService
    def transactional = false

    private static final def SUBSCRIPTION_PATH ='/newsletter/subscription/'

    def addSubscriber(User user) {
        log.info "add user ${user} as newsletter subscriber"
        def body = [
            email: user.email
        ]

        def apiResponse = ApiConsumer.putJson(configurationService.getNewsletterUrl(),
                "${SUBSCRIPTION_PATH}${user.id}", false, new JSONObject(body))
        if(!apiResponse.isOk()){
            log.error "fail to add newsletter subscriber ${user.toString()}"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }

        log.info "successfully add newsletter subscriber ${user.toString()}"
    }

    def removeSubscriber(User user) {
        log.info "remove user ${user} as newsletter subscriber"
        def apiResponse = ApiConsumer.deleteJson(configurationService.getNewsletterUrl(), "${SUBSCRIPTION_PATH}${user.id}")
        if(!apiResponse.isOk()){
            log.error "fail to add remove subscriber ${user.toString()}"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        log.info "successfully remove subscriber ${user.toString()}"
    }

    def isSubscriber(User user) {
        def apiResponse = ApiConsumer.getJson(configurationService.getNewsletterUrl(), "${SUBSCRIPTION_PATH}${user.id}")

        // when 200 return true
        // when 404 return false
        // otherwise throw exeption
        if(!apiResponse.isOk()){
            if(apiResponse.status == ApiResponse.HttpStatus.HTTP_404) {
                log.info "The user ${user.toString()} is _not_ a subscriber"
                return false
            }
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }

        log.info "The user ${user.toString()} is a subscriber"
        return true
    }
}
