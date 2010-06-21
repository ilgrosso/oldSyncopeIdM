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
package org.syncope.core.rest.test;

import org.syncope.client.to.DerivedAttributeSchemaTO;
import java.util.List;
import org.junit.Test;
import org.syncope.client.to.AttributeSchemaTO;
import static org.junit.Assert.*;

public class SchemaTestITCase extends AbstractTestITCase {

    @Test
    public void attributeList() {
        List<AttributeSchemaTO> attributeSchemas =
                restTemplate.getForObject(BASE_URL
                + "schema/attribute/role/list.json", List.class);

        assertNotNull(attributeSchemas);
    }

    @Test
    public void derivedAttributeList() {
        List<DerivedAttributeSchemaTO> derivedAttributeSchemas =
                restTemplate.getForObject(BASE_URL
                + "schema/derivedAttribute/user/list.json", List.class);

        assertNotNull(derivedAttributeSchemas);
    }
}