/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.syncope.console;

import org.junit.Test;

public class AccessTestITCase extends AbstractTest {

    @Test
    public void clickAround() {
        selenium.click("css=img[alt=\"Schema\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='user']\");", "30000");

        selenium.click("//div[@id='user']/ul/li[2]/a/span");
        selenium.click("//div[@id='user']/ul/li[3]/a/span");

        selenium.click("link=Role");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='role']\");", "30000");

        selenium.click("//div[@id='role']/ul/li/a/span");
        selenium.click("//div[@id='role']/ul/li[2]/a/span");
        selenium.click("//div[@id='role']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='membership']\");",
                "30000");

        selenium.click("//div[@id='membership']/ul/li[2]/a/span");
        selenium.click("//div[@id='membership']/ul/li[3]/a/span");

        selenium.click("css=img[alt=\"Users\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("link=Search");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("css=img[alt=\"TODO\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("css=img[alt=\"Reports\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("css=img[alt=\"Configuration\"]");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[@id='tabs']/ul/li[2]/a/span\");",
                "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");

        selenium.click("css=img[alt=\"Tasks\"]");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//div[@id='tabs']/ul/li[2]/a/span\");",
                "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
    }
}
