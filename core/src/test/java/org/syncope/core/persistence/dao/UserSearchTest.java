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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Resource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.user.SyncopeUser;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:syncopeContext.xml",
    "classpath:persistenceContext.xml"
})
@Transactional
public class UserSearchTest {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            UserSearchTest.class);

    @Resource(name = "userSearchDAOCriteriaImpl")
    private UserSearchDAO userSearchCriteriaDAO;

    @Resource(name = "userSearchDAONativeImpl")
    private UserSearchDAO userSearchNativeDAO;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DefaultDataTypeFactory dbUnitDataTypeFactory;

    @Before
    public void createDataAndSearchViews()
            throws Exception {

        Connection conn = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection dbUnitConn = new DatabaseConnection(conn);

        try {
            conn = dataSource.getConnection();

            Statement statement = conn.createStatement();
            statement.executeUpdate("DROP VIEW user_search_attr");
            statement.executeUpdate("DROP VIEW user_search_membership");
            statement.close();
        } catch (SQLException e) {
        }

        DatabaseConfig config = dbUnitConn.getConfig();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                dbUnitDataTypeFactory);

        FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
        dataSetBuilder.setColumnSensing(true);
        IDataSet dataSet = dataSetBuilder.build(getClass().getResourceAsStream(
                "/content.xml"));
        try {
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConn, dataSet);
        } catch (Throwable t) {
            LOG.error("While executing tests", t);
        }

        InputStream viewsStream = UserSearchTest.class.getResourceAsStream(
                "/views.xml");
        Properties views = new Properties();
        views.loadFromXML(viewsStream);

        Statement statement = null;
        for (String idx : views.stringPropertyNames()) {
            LOG.debug("Creating view {}", views.get(idx).toString());

            try {
                statement = conn.createStatement();
                statement.executeUpdate(views.get(idx).toString().
                        replaceAll("\\n", " "));
                statement.close();
            } catch (SQLException e) {
                LOG.error("Could not create view ", e);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        DataSourceUtils.releaseConnection(conn, dataSource);
    }

    @Test
    public final void searchWithLikeCondition() {
        searchWithLikeCondition(userSearchCriteriaDAO);
        searchWithLikeCondition(userSearchNativeDAO);
    }

    private void searchWithLikeCondition(UserSearchDAO searchDAO) {
        AttributeCond usernameLeafCond =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(membershipCond));

        assertTrue(subCond.checkValidity());

        NodeCond cond = NodeCond.getAndCond(subCond,
                NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.checkValidity());

        List<SyncopeUser> users = searchDAO.search(cond);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public final void searchWithNotCondition() {
        searchWithNotCondition(userSearchCriteriaDAO);
        searchWithNotCondition(userSearchNativeDAO);
    }

    private void searchWithNotCondition(UserSearchDAO searchDAO) {
        final AttributeCond usernameLeafCond =
                new AttributeCond(AttributeCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("fabio.martelli");

        final NodeCond cond = NodeCond.getNotLeafCond(usernameLeafCond);
        assertTrue(cond.checkValidity());

        final List<SyncopeUser> users = searchDAO.search(cond);
        assertNotNull(users);
        assertEquals(2, users.size());

        Set<Long> ids = new HashSet<Long>(2);
        ids.add(users.get(0).getId());
        ids.add(users.get(1).getId());
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(4L));
    }

    @Test
    public final void searchByBoolean() {
        searchByBoolean(userSearchCriteriaDAO);
        searchByBoolean(userSearchNativeDAO);
    }

    private void searchByBoolean(UserSearchDAO searchDAO) {
        final AttributeCond coolLeafCond =
                new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        final NodeCond cond = NodeCond.getLeafCond(coolLeafCond);
        assertTrue(cond.checkValidity());

        final List<SyncopeUser> users = searchDAO.search(cond);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getId());
    }

    @Test
    public final void searchByPageAndSize() {
        searchByPageAndSize(userSearchCriteriaDAO);
        searchByPageAndSize(userSearchNativeDAO);
    }

    private void searchByPageAndSize(UserSearchDAO searchDAO) {
        AttributeCond usernameLeafCond =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(membershipCond));

        assertTrue(subCond.checkValidity());

        NodeCond cond = NodeCond.getAndCond(subCond,
                NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.checkValidity());

        List<SyncopeUser> users = searchDAO.search(cond, 1, 2, null);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(cond, 2, 2, null);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public final void searchByMembership() {
        searchByMembership(userSearchCriteriaDAO);
        searchByMembership(userSearchNativeDAO);
    }

    private void searchByMembership(UserSearchDAO searchDAO) {
        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        List<SyncopeUser> users =
                searchDAO.search(NodeCond.getLeafCond(membershipCond));
        assertNotNull(users);
        assertEquals(2, users.size());

        membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        users = searchDAO.search(NodeCond.getNotLeafCond(membershipCond));
        assertNotNull(users);
        assertEquals(3, users.size());
    }

    @Test
    public void searchByIsNull() {
        searchByIsNull(userSearchCriteriaDAO);
        searchByIsNull(userSearchNativeDAO);
    }

    private void searchByIsNull(UserSearchDAO searchDAO) {
        AttributeCond coolLeafCond =
                new AttributeCond(AttributeCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<SyncopeUser> users =
                searchDAO.search(NodeCond.getLeafCond(coolLeafCond));
        assertNotNull(users);
        assertEquals(3, users.size());

        coolLeafCond =
                new AttributeCond(AttributeCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users =
                searchDAO.search(NodeCond.getLeafCond(coolLeafCond));
        assertNotNull(users);
        assertEquals(1, users.size());
    }
}
