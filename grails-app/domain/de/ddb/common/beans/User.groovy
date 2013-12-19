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
package de.ddb.common.beans

import org.apache.commons.lang.StringUtils

class User implements Cloneable {

    public final static String SESSION_USER = "SESSION_USER_ATTRIBUTE"

    String id
    String username
    String status
    String firstname
    String lastname
    String email
    String password
    boolean openIdUser
    boolean newsletterSubscribed
    String apiKey

    // TODO: why do we need to declare a default constructor?
    // TODO: *arb*: If you write a Question in a TODO, write your name after it so we know who to ask remove the TODO :)
    User(){
    }

    public String toString() {
        return "User[id=${id}, username=${username}, firstname=${firstname}, lastname=${lastname}, email=${email}, openIdUser=${openIdUser}, isSubscriber=${newsletterSubscribed}, apiKey=${apiKey}]"
    }

    // Utility method to get first name and last name if present or username if not
    public String getFirstnameAndLastnameOrNickname() {
        if (firstname || lastname) {
            if (!lastname) {
                return firstname
            } else if (!firstname) {
                return lastname
            } else {
                return firstname+" "+lastname
            }
        } else {
            return username
        }
    }

    // Utility method to check if attributes are consistent
    public boolean isConsistent() {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(username) || StringUtils.isBlank(email)) {
            return false
        }
        if (!openIdUser) {
            //additional attributes for non-openid users
            if (StringUtils.isBlank(password)) {
                return false
            }
        }
        return true
    }

    public Object clone() throws CloneNotSupportedException {
        Object result = super.clone()
        return result
    }
}
