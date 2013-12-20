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

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

import org.codehaus.groovy.grails.web.util.WebUtils

class SessionService {

    def transactional = false

    public HttpSession getSessionIfAvailable() {
        HttpServletRequest request = WebUtils.retrieveGrailsWebRequest().getCurrentRequest()
        return request.getSession(false)
    }

    public HttpSession createNewSession() {
        HttpServletRequest request = WebUtils.retrieveGrailsWebRequest().getCurrentRequest()
        return request.getSession(true)
    }

    public void destroySession() {
        HttpSession session = getSessionIfAvailable()
        if(session){
            session.invalidate()
        }
    }

    public boolean isSessionAvailable() {
        HttpSession session = getSessionIfAvailable()
        if(session){
            return true
        }else{
            return false
        }
    }

    public Object getSessionAttributeIfAvailable(String attributeName) {
        HttpSession session = getSessionIfAvailable()
        if(session){
            return session.getAttribute(attributeName)
        }else{
            return null
        }
    }

    public void setSessionAttributeIfAvailable(String attributeName, Object value) {
        HttpSession session = getSessionIfAvailable()
        if(session){
            session.setAttribute(attributeName, value)
        }
    }

    public void setSessionAttribute(HttpSession session, String attributeName, Object value) {
        if(session){
            session.setAttribute(attributeName, value)
        }
    }

    public void removeSessionAttributeIfAvailable(String attributeName) {
        HttpSession session = getSessionIfAvailable()
        if(session){
            session.removeAttribute(attributeName)
        }
    }
}
