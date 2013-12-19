package de.ddb.common

class EchoService {
    def transactional = false

    def echo(String input) {
        return input
    }
}
