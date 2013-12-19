package de.ddb.common

import grails.test.mixin.TestFor

import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(EchoService)
class EchoServiceTests extends GroovyTestCase {

    @Test void echoTest() {
        String echo = service.echo("Hallo Echo")

        assert echo == "Hallo Echo"
    }
}
