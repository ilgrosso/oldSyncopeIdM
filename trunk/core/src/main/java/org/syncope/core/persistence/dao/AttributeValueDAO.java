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
package org.syncope.core.persistence.dao;

import java.util.List;
import org.syncope.core.persistence.beans.AbstractAttributeValue;

public interface AttributeValueDAO extends DAO {

    <T extends AbstractAttributeValue> T find(Long id, Class<T> reference);

    <T extends AbstractAttributeValue> List<T> findAll(Class<T> reference);

    <T extends AbstractAttributeValue> boolean existingAttributeValue(
            T attributeValue);

    <T extends AbstractAttributeValue> T save(T attributeValue);

    <T extends AbstractAttributeValue> void delete(Long id, Class<T> reference);

    <T extends AbstractAttributeValue> void delete(T attributeValue);
}