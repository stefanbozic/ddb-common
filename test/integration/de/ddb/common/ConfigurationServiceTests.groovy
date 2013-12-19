package de.ddb.common

import grails.test.mixin.TestFor

import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ConfigurationService)
class ConfigurationServiceTests extends GroovyTestCase {

    @Test void getServiceTest() {
        assert service != null
    }

    @Test void getBackendTest() {
        String echo = service.getBackendUrl()

        println echo
    }
}
