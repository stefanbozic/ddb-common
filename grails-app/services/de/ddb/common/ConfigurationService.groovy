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

import org.codehaus.groovy.grails.web.mapping.LinkGenerator

import de.ddb.common.exception.ConfigurationException

/**
 * Service for accessing the configuration.
 *
 * @author hla
 */
class ConfigurationService {

    def grailsApplication
    def LinkGenerator grailsLinkGenerator

    def transactional=false

    public String getBinaryUrl() {
        return getConfigValue("ddb.binary.url")
    }

    public String getStaticUrl(){
        return getConfigValue("ddb.static.url")
    }

    public String getApisUrl(){
        return getConfigValue("ddb.apis.url")
    }

    public String getBackendUrl(){
        return getConfigValue("ddb.backend.url")
    }

    public String getAasUrl(){
        return getConfigValue("ddb.aas.url")
    }

    public String getCulturegraphUrl(){
        return getConfigValue("ddb.culturegraph.url")
    }

    public String getBookmarkUrl(){
        return getConfigValue("ddb.bookmark.url")
    }

    public String getNewsletterUrl(){
        return getConfigValue("ddb.newsletter.url")
    }

    public String getElasticSearchUrl(){
        return getConfigValue("ddb.elasticsearch.url")
    }

    /**
     * Return the application base URL with context path and without trailing slash.
     */
    public String getContextUrl(){
        return grailsLinkGenerator.serverBaseURL
    }

    /**
     * Return the application base URL without context path and without trailing slash.
     */
    public String getSelfBaseUrl(){
        def result = getContextUrl()
        if (grailsLinkGenerator.contextPath?.length() > 0) {
            result = result.substring(0, result.length() - grailsLinkGenerator.contextPath.length())
        }
        return result
    }

    public String getConfirmBase(){
        return getContextUrl() + "/user/confirm/|id|/|confirmationToken|"
    }

    public String getPasswordResetConfirmationLink(){
        return getConfirmBase() + "?type=passwordreset"
    }

    public String getEmailUpdateConfirmationLink(){
        return getConfirmBase() + "?type=emailupdate"
    }

    public String getCreateConfirmationLink(){
        return getConfirmBase() + "?type=create"
    }

    public String getFavoritesSendMailFrom(){
        return getConfigValue("ddb.favorites.sendmailfrom")
    }

    public String getFavoritesReportMailTo(){
        return getConfigValue("ddb.favorites.reportMailTo")
    }

    public List getFacetsFilter(){
        return getConfigValue("ddb.backend.facets.filter", List)
    }

    public String getPiwikTrackingFile(){
        return getExistingConfigValue("ddb.tracking.piwikfile")
    }

    public String getApiKeyDocUrl(){
        return getConfigValue("ddb.apikey.doc.url")
    }

    public String getApiKeyTermsUrl(){
        return getConfigValue("ddb.apikey.terms.url")
    }

    public String getAccountTermsUrl(){
        return getConfigValue("ddb.account.terms.url")
    }

    public String getAccountPrivacyUrl(){
        return getConfigValue("ddb.account.privacy.url")
    }

    public String getEncoding(){
        return getConfigValue("grails.views.gsp.encoding")
    }

    public String getMimeTypeHtml(){
        return getConfigValue("grails.mime.types['html'][0]", String, grailsApplication.config.grails?.mime?.types["html"][0])
    }

    public String getLoggingFolder(){
        return getConfigValue("ddb.logging.folder")
    }

    public String getLoadbalancerHeaderName(){
        return getConfigValue("ddb.loadbalancer.header.name")
    }

    public String getLoadbalancerHeaderValue(){
        return getConfigValue("ddb.loadbalancer.header.value")
    }

    public String getGrailsMailHost(){
        return getConfigValue("grails.mail.host")
    }

    public String getProxyHost(){
        return getSystemProperty("http.proxyHost")
    }

    public String getProxyPort(){
        return getSystemProperty("http.proxyPort")
    }

    public String getNonProxyHosts(){
        return getSystemProperty("http.nonProxyHosts")
    }


    /**
     * Get the authorization key to access restricted API calls.
     *
     * This property is optional. Leave it blank if you do not want to set an API key.
     *
     * @return the authorization key
     */
    public String getBackendApikey(){
        return getProperlyTypedConfigValue("ddb.backend.apikey")
    }

    public int getSearchGroupCount() {
        return getIntegerConfigValue("ddb.advancedSearch.searchGroupCount")
    }

    public int getSearchFieldCount() {
        return getIntegerConfigValue("ddb.advancedSearch.searchFieldCount")
    }

    public int getSearchOffset() {
        return getIntegerConfigValue("ddb.advancedSearch.defaultOffset")
    }

    public int getSearchRows() {
        return getIntegerConfigValue("ddb.advancedSearch.defaultRows")
    }

    public int getSessionTimeout() {
        return getIntegerConfigValue("ddb.session.timeout")
    }

    public int getGrailsMailPort() {
        return getIntegerConfigValue("grails.mail.port")
    }

    public boolean isCulturegraphFeaturesEnabled() {
        def value = getExistingConfigValue("ddb.culturegraph.features.enabled")
        return Boolean.parseBoolean(value.toString())
    }

    public def logConfigurationSettings() {
        log.info "------------- System.properties -----------------------"
        log.info "proxyHost = " + getProxyHost()
        log.info "proxyPort = " + getProxyPort()
        log.info "nonProxyHosts = " + getNonProxyHosts()
        log.info "------------- application.properties ------------------"
        log.info "app.grails.version = "+grailsApplication.metadata["app.grails.version"]
        log.info "app.name = "+grailsApplication.metadata["app.name"]
        log.info "app.version = "+grailsApplication.metadata["app.version"]
        log.info "build.number = "+grailsApplication.metadata["build.number"]
        log.info "build.id = "+grailsApplication.metadata["build.id"]
        log.info "build.url = "+grailsApplication.metadata["build.url"]
        log.info "build.git.commit = "+grailsApplication.metadata["build.git.commit"]
        log.info "build.bit.branch = "+grailsApplication.metadata["build.bit.branch"]
        log.info "------------- ddb-next.properties ---------------------"
        log.info "ddb.binary.url = " + getBinaryUrl()
        log.info "ddb.static.url = " + getStaticUrl()
        log.info "ddb.apis.url = " + getApisUrl()
        log.info "ddb.backend.url = " + getBackendUrl()
        log.info "ddb.backend.apikey = " + getBackendApikey()
        log.info "ddb.aas.url = " + getAasUrl()
        log.info "ddb.culturegraph.url = " + getCulturegraphUrl()
        log.info "ddb.bookmark.url = " + getBookmarkUrl()
        log.info "ddb.newsletter.url = " + getNewsletterUrl()
        log.info "ddb.favorites.sendmailfrom = " + getFavoritesSendMailFrom()
        log.info "ddb.favorites.reportMailTo = " + getFavoritesReportMailTo()
        log.info "ddb.backend.facets.filter = " + getFacetsFilter()
        log.info "ddb.tracking.piwikfile = " + getPiwikTrackingFile()
        log.info "grails.views.gsp.encoding = " + getEncoding()
        log.info "grails.mime.types['html'][0] = " + getMimeTypeHtml()
        log.info "ddb.advancedSearch.searchGroupCount = " + getSearchGroupCount()
        log.info "ddb.advancedSearch.searchFieldCount = " + getSearchFieldCount()
        log.info "ddb.advancedSearch.defaultOffset = " + getSearchOffset()
        log.info "ddb.advancedSearch.defaultRows = " + getSearchRows()
        log.info "ddb.session.timeout = " + getSessionTimeout()
        log.info "ddb.logging.folder = " + getLoggingFolder()
        log.info "ddb.loadbalancer.header.name = " + getLoadbalancerHeaderName()
        log.info "ddb.loadbalancer.header.value = " + getLoadbalancerHeaderValue()
        log.info "ddb.elasticsearch.url = " + getElasticSearchUrl()
        log.info "ddb.culturegraph.features.enabled = " + isCulturegraphFeaturesEnabled()
        log.info "ddb.apikey.doc.url = " + getApiKeyDocUrl()
        log.info "ddb.apikey.terms.url = " + getApiKeyTermsUrl()
        log.info "ddb.account.terms.url = " + getAccountTermsUrl()
        log.info "ddb.account.privacy.url = " + getAccountPrivacyUrl()
        log.info "grails.mail.host = " + getGrailsMailHost()
        log.info "grails.mail.port = " + getGrailsMailPort()
        log.info "getContextUrl = " + getContextUrl()
        log.info "getSelfBaseUrl = " + getSelfBaseUrl()
        log.info "-------------------------------------------------------"
    }

    private String getSystemProperty(String key) {
        String propertyValue = System.getProperty(key)
        if(!propertyValue){
            log.warn "No " + key + " configured -> System.getProperty('" + key + "'). This will most likely lead to problems."
        }
        return propertyValue
    }

    private def getValueFromConfig(String key) {
        def value = grailsApplication.config
        for (String keyPart : key.split("\\.")) {
            if (!(value instanceof ConfigObject)) {
                value = null
                break
            }
            value = value[keyPart]
        }
        try {
            if (value?.isEmpty()) {
                value = null
            }
        }
        catch (MissingMethodException e) {
        }
        return value
    }

    private def getConfigValue(String key, Class expectedClass = String, def value = getValueFromConfig(key)) {
        getExistingConfigValue(key, value)
        return getProperlyTypedConfigValue(key, expectedClass, value)
    }

    private def getProperlyTypedConfigValue(String key, Class expectedClass = String, def value = getValueFromConfig(key)) {
        if(!expectedClass.isAssignableFrom(value.getClass())){
            throw new ConfigurationException(key + " is not a " + expectedClass.getSimpleName())
        }
        return value
    }

    private def getExistingConfigValue(String key, def value = getValueFromConfig(key)) {
        if(value == null){
            throw new ConfigurationException("Configuration entry does not exist -> " + key)
        }
        return value
    }

    private Integer getIntegerConfigValue(String key, def value = getValueFromConfig(key)) {
        def searchGroupCount = getExistingConfigValue(key, value)
        return parseIntegerValue(key, searchGroupCount)
    }

    private Integer parseIntegerValue(String key, def value) {
        try {
            return Integer.parseInt(value.toString())
        }
        catch (NumberFormatException e) {
            throw new ConfigurationException(key + " is not an Integer")
        }
    }
}
