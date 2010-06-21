/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.syncope.console.pages.HomePage;
import org.syncope.console.pages.Login;
import org.syncope.console.rest.RestClient;

/**
 * SyncopeApplication class.
 */
public class SyncopeApplication extends WebApplication
{
    SyncopeUser user = null;
    RestClient restClient;

    public SyncopeApplication()
    {
    }

    @Override
    protected void init()
    {
        getResourceSettings().setThrowExceptionOnMissingResource( true );

    }

    /**
     * Create a new custom SyncopeSession
     * @param request
     * @param response
     * @return Session
     */
    @Override
    public Session newSession( Request request, Response response )
    {

        SyncopeSession session = new SyncopeSession( request );
        
        if ( user != null )
        {
            session.setUser( user );
        }

        return session;
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    public Class getHomePage()
    {
        return (user == null) ? Login.class :  HomePage.class;
    }

    /**
     * Use this method to switch from DEVELOPMENT to DEPLOYMENT mode
     * on production enviroment.
     *
     * @return String : Configuration type
     */
    @Override
    public String getConfigurationType()
    {
        return DEVELOPMENT;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

}