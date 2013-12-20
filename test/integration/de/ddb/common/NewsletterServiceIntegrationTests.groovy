package de.ddb.common

import static org.junit.Assert.*

import org.junit.*

import de.ddb.common.beans.User

class NewsletterServiceIntegrationTests extends GroovyTestCase {

    def newsletterService

    @Ignore("Newsletter feature is disabled temporary.")
    @Test
    void shouldAddUserAsSubscriber() {
        User user = new User(email: 'john.doe@example.com')
        def userId = UUID.randomUUID() as String
        user.setId(userId)
        newsletterService.addSubscriber(user)
        assert newsletterService.isSubscriber(user) == true
    }

    @Ignore("Newsletter feature is disabled temporary.")
    @Test
    void shouldRemoveUserAsSubscriber() {
        User user = new User(email: 'john.doe@example.com')
        def userId = UUID.randomUUID() as String
        user.setId(userId)
        newsletterService.addSubscriber(user)
        assert newsletterService.isSubscriber(user) == true

        newsletterService.removeSubscriber(user)
        assert newsletterService.isSubscriber(user) == false
    }

    @Ignore("Newsletter feature is disabled temporary.")
    @Test
    void shouldReturnFalseIfUserIsNotSubscriber() {
        User user = new User(email: 'john.doe@example.com')
        def userId = UUID.randomUUID() as String
        user.setId(userId)
        assert newsletterService.isSubscriber(user) == false
    }
}
