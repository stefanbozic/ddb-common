
package ddb.common.services;

import grails.test.mixin.*

import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication

import de.ddb.common.ConfigurationService
import de.ddb.common.exception.ConfigurationException

@TestFor(ConfigurationService)
class ConfigurationServiceTests {

    static final String EMPTY_STRING_VALUE = ""
    static final String STRING_VALUE = "string value"
    static final Object OBJECT_VALUE = new Object()
    static final List LIST_VALUE = ["list value"]

    static final int INTEGER_VALUE = 42
    static final String INTEGER_VALUE_AS_STRING = "42"

    static final boolean BOOLEAN_VALUE = true
    static final String BOOLEAN_VALUE_AS_STRING = "true"

    private ConfigurationService service
    private String lastLog
    private String key
    private Closure configCall

    void testGetBinaryUrl_Complete() {
        stringConfigTest("ddb.binary.url") { it.getBinaryUrl() }
    }

    void testGetPartiallyDefinedEntry() {
        key = "ddb.binary.url"
        configCall = { it.getBinaryUrl() }
        setupService(STRING_VALUE, "ddb.binary")
        configTest_NotFound()
    }

    void testGetStaticUrl_Complete() {
        stringConfigTest("ddb.static.url", { it.getStaticUrl() })
    }

    void testGetApisUrl_Complete() {
        stringConfigTest("ddb.apis.url", { it.getApisUrl() })
    }

    void testGetBackendUrl_Complete() {
        stringConfigTest("ddb.backend.url", { it.getBackendUrl() })
    }

    void testGetAasUrl_Complete() {
        stringConfigTest("ddb.aas.url", { it.getAasUrl() })
    }

    void testGetCulturegraphUrl_Complete() {
        stringConfigTest("ddb.culturegraph.url", { it.getCulturegraphUrl() })
    }

    void testGetBookmarkUrl_Complete() {
        stringConfigTest("ddb.bookmark.url", { it.getBookmarkUrl() })
    }

    void testGetNewsletterUrl_Complete() {
        stringConfigTest("ddb.newsletter.url", { it.getNewsletterUrl() })
    }

    void testGetElasticSearchUrl_Complete() {
        stringConfigTest("ddb.elasticsearch.url", { it.getElasticSearchUrl() })
    }

    void testGetFavoritesSendMailFrom_Complete() {
        stringConfigTest("ddb.favorites.sendmailfrom", { it.getFavoritesSendMailFrom() })
    }

    void testGetFavoritesReportMailTo_Complete() {
        stringConfigTest("ddb.favorites.reportMailTo", { it.getFavoritesReportMailTo() })
    }

    void testGetApiKeyDocUrl_Complete() {
        stringConfigTest("ddb.apikey.doc.url", { it.getApiKeyDocUrl() })
    }

    void testGetApiKeyTermsUrl_Complete() {
        stringConfigTest("ddb.apikey.terms.url", { it.getApiKeyTermsUrl() })
    }

    void testGetAccountTermsUrl_Complete() {
        stringConfigTest("ddb.account.terms.url", { it.getAccountTermsUrl() })
    }

    void testGetAccountPrivacyUrl_Complete() {
        stringConfigTest("ddb.account.privacy.url", { it.getAccountPrivacyUrl() })
    }

    void testGetEncoding_Complete() {
        stringConfigTest("grails.views.gsp.encoding", { it.getEncoding() })
    }

    void testLoggingFolder_Complete() {
        stringConfigTest("ddb.logging.folder", { it.getLoggingFolder() })
    }

    void testLoadbalancerHeaderName_Complete() {
        stringConfigTest("ddb.loadbalancer.header.name", { it.getLoadbalancerHeaderName() })
    }

    void testLoadbalancerHeaderValue_Complete() {
        stringConfigTest("ddb.loadbalancer.header.value", { it.getLoadbalancerHeaderValue() })
    }

    void testGrailsMailHost_Complete() {
        stringConfigTest("grails.mail.host", { it.getGrailsMailHost() })
    }

    void testMimeTypeHtml() {
        key = "grails.mime.types"
        configCall = { it.getMimeTypeHtml() }
        setupService(createMimetypeValueMap(STRING_VALUE))
        configTest_Success()

        setupServiceWithoutConfigValues()
        configTest_NotFound("grails.mime.types['html'][0]")

        setupService(createMimetypeValueMap(OBJECT_VALUE))
        configTest_WrongType("a String", "grails.mime.types['html'][0]")
    }

    void testFacetsFilter_Complete() {
        key = "ddb.backend.facets.filter"
        configCall = { it.getFacetsFilter() }
        setupService(LIST_VALUE)
        configTest_Success(LIST_VALUE)

        setupServiceWithoutConfigValues()
        configTest_NotFound()

        setupService(OBJECT_VALUE)
        configTest_WrongType("a List")
    }

    void testSearchGroupCount_Complete() {
        integerConfigTest("ddb.advancedSearch.searchGroupCount") { it.getSearchGroupCount() }
    }

    void testSearchFieldCount_Complete() {
        integerConfigTest("ddb.advancedSearch.searchFieldCount") { it.getSearchFieldCount() }
    }

    void testSearchOffset_Complete() {
        integerConfigTest("ddb.advancedSearch.defaultOffset") { it.getSearchOffset() }
    }

    void testSearchRows_Complete() {
        integerConfigTest("ddb.advancedSearch.defaultRows") { it.getSearchRows() }
    }

    void testSessionTimeout_Complete() {
        integerConfigTest("ddb.session.timeout") { it.getSessionTimeout() }
    }

    void testGrailsMailPort_Complete() {
        integerConfigTest("grails.mail.port") { it.getGrailsMailPort() }
    }

    void testIsCulturegraphFeaturesEnabled_Complete() {
        booleanConfigTest("ddb.culturegraph.features.enabled") { it.isCulturegraphFeaturesEnabled() }
    }

    void testGetPiwikTrackingFile_Complete() {
        key = "ddb.tracking.piwikfile"
        configCall = { it.getPiwikTrackingFile() }
        setupService(STRING_VALUE)
        configTest_Success()

        setupService(OBJECT_VALUE)
        configTest_Success(OBJECT_VALUE.toString())

        setupServiceWithoutConfigValues()
        configTest_NotFound()

        setupService(EMPTY_STRING_VALUE)
        configTest_NotFound()
    }

    void testGetProxyHost() {
        systemPropertyTest("http.proxyHost") { it.getProxyHost() }
    }

    void testGetProxyPort() {
        systemPropertyTest("http.proxyPort") { it.getProxyPort() }
    }

    void testGetNonProxyHosts() {
        systemPropertyTest("http.nonProxyHosts") { it.getNonProxyHosts() }
    }

    void testGetBackendApikey_Complete() {
        key = "ddb.backend.apikey"
        configCall = { it.getBackendApikey() }
        setupService(STRING_VALUE)
        configTest_Success()

        setupService(OBJECT_VALUE)
        configTest_WrongType()

        setupServiceWithoutConfigValues()
        configTest_WrongType()
    }

    private Map createMimetypeValueMap(def value) {
        def map = [:]
        map["html"] = []
        map["html"][0] = value
        return map
    }

    private void systemPropertyTest(String key, Closure configCall) {
        this.key = key
        this.configCall = configCall
        setupServiceWithoutConfigValues()
        setSystemProperty(STRING_VALUE) { configTest_Success() }
        setSystemProperty(null) {
            setupServiceWithLogMock()
            configCall(service)
            assert lastLog == "No " + key + " configured -> System.getProperty('" + key + "'). This will most likely lead to problems."
        }
    }

    private booleanConfigTest(String key, Closure configCall) {
        this.key = key
        this.configCall = configCall
        setupService(BOOLEAN_VALUE)
        configTest_Success(BOOLEAN_VALUE)

        setupService(BOOLEAN_VALUE_AS_STRING)
        configTest_Success(BOOLEAN_VALUE)

        setupServiceWithoutConfigValues()
        configTest_NotFound()

        setupService(EMPTY_STRING_VALUE)
        configTest_NotFound()

        setupService(OBJECT_VALUE)
        configTest_Success(false)

        setupService(STRING_VALUE)
        configTest_Success(false)
    }

    private integerConfigTest(String key, Closure configCall) {
        this.key = key
        this.configCall = configCall
        setupService(INTEGER_VALUE)
        configTest_Success(INTEGER_VALUE)

        setupService(INTEGER_VALUE_AS_STRING)
        configTest_Success(INTEGER_VALUE)

        setupServiceWithoutConfigValues()
        configTest_NotFound()

        setupService(EMPTY_STRING_VALUE)
        configTest_NotFound()

        setupService(OBJECT_VALUE)
        configTest_WrongType("an Integer")

        setupService(STRING_VALUE)
        configTest_WrongType("an Integer")
    }

    private setSystemProperty(String propertyValue, Closure runnable) {
        String oldPropertyValue = System.getProperty(key)
        setPropertyValue(key, propertyValue)
        runnable()
        setPropertyValue(key, oldPropertyValue)
    }

    private setPropertyValue(String propertyKey, String propertyValue) {
        if (propertyValue) {
            System.setProperty(propertyKey, propertyValue)
        } else {
            System.clearProperty(propertyKey)
        }
    }

    private void stringConfigTest(String key, Closure configCall) {
        this.key = key
        this.configCall = configCall
        setupService(STRING_VALUE)
        configTest_Success()

        setupServiceWithoutConfigValues()
        configTest_NotFound()

        setupService(EMPTY_STRING_VALUE)
        configTest_NotFound()

        setupService(OBJECT_VALUE)
        configTest_WrongType()
    }

    private configTest_WrongType(String typeName = "a String", String key = this.key) {
        try {
            configCall(service)
            fail()
        } catch (ConfigurationException e) {
            assert e.getMessage().startsWith(key + " is not a")
        }
    }

    private String configTest_NotFound(String key = this.key) {
        try {
            configCall(service)
            fail()
        } catch (ConfigurationException e) {
            assert e.getMessage() == "Configuration entry does not exist -> " + key
        }
    }

    private configTest_Success(Object expectedValue = STRING_VALUE) {
        assert configCall(service) == expectedValue
    }

    private ConfigurationService setupService(Object value, String key = this.key) {
        service = new ConfigurationService()
        service.grailsApplication = createGrailsApplication(key, value)
        return service
    }

    private ConfigurationService setupServiceWithoutConfigValues() {
        service = new ConfigurationService()
        service.grailsApplication = createGrailsApplication("empty", "undefined")
        return service
    }

    private ConfigurationService setupServiceWithLogMock() {
        service = new ConfigurationService()
        service.log = createLogMock()
        return service
    }

    private def createLogMock() {
        def logGenerator = mockFor(Log)
        logGenerator.demand.warn { String msg -> lastLog = msg }
        return logGenerator.createMock()
    }

    private GrailsApplication createGrailsApplication(String key, Object value) {
        def grailsApplication = new DefaultGrailsApplication()
        def configProperties = new Properties()
        configProperties.put(key, value)
        def config = new ConfigSlurper().parse(configProperties)
        grailsApplication.setConfig(config)
        return grailsApplication
    }
}
