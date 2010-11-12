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
package org.syncope.core.persistence.dao.impl;

import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.validation.ValidationException;

@Repository
public class SyncopeUserDAOImpl extends AbstractDAOImpl
        implements SyncopeUserDAO {

    @Autowired
    private SchemaDAO schemaDAO;

    @Override
    @Transactional(readOnly = true)
    public SyncopeUser find(Long id) {
        return entityManager.find(SyncopeUser.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public SyncopeUser findByWorkflowId(Long workflowId) {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeUser e "
                + "WHERE e.workflowId = :workflowId");
        query.setParameter("workflowId", workflowId);

        return (SyncopeUser) query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncopeUser> findByAttributeValue(
            UserAttributeValue attributeValue) {

        Query query = entityManager.createQuery(
                "SELECT u"
                + " FROM SyncopeUser u, UserAttribute ua, UserAttributeValue e "
                + " WHERE e.attribute = ua AND ua.owner = u"
                + " AND ((e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL"
                + " AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL"
                + " AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL"
                + " AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL"
                + " AND e.doubleValue = :doubleValue))");
        query.setParameter("stringValue", attributeValue.getStringValue());
        query.setParameter("booleanValue", attributeValue.getBooleanValue());
        query.setParameter("dateValue", attributeValue.getDateValue());
        query.setParameter("longValue", attributeValue.getLongValue());
        query.setParameter("doubleValue", attributeValue.getDoubleValue());

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncopeUser> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeUser e");
        return query.getResultList();
    }

    @Override
    public SyncopeUser save(SyncopeUser syncopeUser) {
        return entityManager.merge(syncopeUser);
    }

    @Override
    public void delete(Long id) {
        SyncopeUser user = find(id);
        if (user == null) {
            return;
        }

        for (Membership membership : user.getMemberships()) {
            membership.setSyncopeUser(null);
            membership.getSyncopeRole().removeMembership(membership);
            membership.setSyncopeRole(null);

            entityManager.remove(membership);
        }
        user.setMemberships(Collections.EMPTY_LIST);

        entityManager.remove(user);
    }

    @Override
    @Transactional(readOnly = true)
    public final List<SyncopeUser> search(final NodeCond searchCondition) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Search condition:\n" + searchCondition);
        }

        Session hibernateSess = ((Session) entityManager.getDelegate());

        List<SyncopeUser> result = Collections.EMPTY_LIST;
        try {
            result = doSearch(searchCondition);
        } catch (Throwable t) {
            LOG.error("While searching users", t);
        }

        return result;
    }

    @Transactional(readOnly = true)
    private List<SyncopeUser> doSearch(final NodeCond nodeCond) {
        List<SyncopeUser> result = null;
        List<SyncopeUser> rightResult = null;

        switch (nodeCond.getType()) {
            case LEAF:
            case NOT_LEAF:
                Criteria criteria = getBaseCriteria().
                        add(getCriterion(nodeCond));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Criteria to be performed:\n" + criteria);
                }

                result = criteria.list();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Criteria result:\n" + result);
                }
                break;

            case AND:
                result = doSearch(nodeCond.getLeftNodeCond());
                rightResult = doSearch(nodeCond.getRightNodeCond());
                result.retainAll(rightResult);
                break;

            case OR:
                result = doSearch(nodeCond.getLeftNodeCond());
                rightResult = doSearch(nodeCond.getRightNodeCond());
                result.addAll(rightResult);
                break;

            default:
        }

        return result;
    }

    @Transactional(readOnly = true)
    private Criteria getBaseCriteria() {
        Session hibernateSess = (Session) entityManager.getDelegate();
        Criteria baseCriteria = hibernateSess.createCriteria(SyncopeUser.class).
                createAlias("memberships", "m").
                createAlias("m.syncopeRole", "r").
                createAlias("attributes", "a").
                createAlias("a.values", "av");

        baseCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return baseCriteria;
    }

    @Transactional(readOnly = true)
    private Criterion getCriterion(final NodeCond leafCond) {
        Criterion criterion = null;

        switch (leafCond.getType()) {
            case LEAF:
                if (leafCond.getMembershipCond() != null) {
                    if (leafCond.getMembershipCond().getRoleId() != null) {
                        criterion = Restrictions.eq("r.id",
                                leafCond.getMembershipCond().getRoleId());
                    }
                    if (leafCond.getMembershipCond().getRoleName() != null) {
                        criterion = Restrictions.eq("r.name",
                                leafCond.getMembershipCond().getRoleName());
                    }
                } else if (leafCond.getAttributeCond() != null) {
                    UserSchema userSchema = schemaDAO.find(
                            leafCond.getAttributeCond().getSchema(),
                            UserSchema.class);
                    if (userSchema == null) {
                        LOG.warn("Ignoring invalid schema '"
                                + leafCond.getAttributeCond().getSchema()
                                + "'");
                    } else {
                        if (leafCond.getAttributeCond().getType()
                                == AttributeCond.Type.ISNULL) {

                            criterion = Restrictions.and(
                                    Restrictions.eq("a.schema.name",
                                    leafCond.getAttributeCond().getSchema()),
                                    Restrictions.isEmpty("a.values"));
                        } else {
                            try {
                                UserAttributeValue example =
                                        userSchema.getValidator().
                                        getValue(leafCond.getAttributeCond().
                                        getExpression(),
                                        new UserAttributeValue());
                                criterion = Restrictions.and(
                                        Restrictions.eq("a.schema.name",
                                        leafCond.getAttributeCond().
                                        getSchema()),
                                        getCriterion(
                                        leafCond.getAttributeCond().getType(),
                                        example));
                            } catch (ValidationException e) {
                                LOG.error("Could not validate expression '"
                                        + leafCond.getAttributeCond().
                                        getExpression() + "'", e);
                            }
                        }
                    }

                }


                break;

            case NOT_LEAF:
                leafCond.setType(NodeCond.Type.LEAF);
                criterion = Restrictions.not(getCriterion(leafCond));
                break;
        }

        return criterion;
    }

    @Transactional(readOnly = true)
    private Criterion getCriterion(final AttributeCond.Type type,
            final AbstractAttributeValue example) {

        Criterion result = null;
        switch (type) {
            case EQ:
                result = Restrictions.disjunction().
                        add(Restrictions.eq("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.eq("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.eq("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.eq("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.eq("av.dateValue",
                        example.getDateValue()));
                break;

            case GE:
                result = Restrictions.disjunction().
                        add(Restrictions.ge("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.ge("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.ge("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.ge("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.ge("av.dateValue",
                        example.getDateValue()));
                break;

            case GT:
                result = Restrictions.disjunction().
                        add(Restrictions.gt("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.gt("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.gt("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.gt("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.gt("av.dateValue",
                        example.getDateValue()));
                break;

            case ISNOTNULL:
                result = Restrictions.disjunction().
                        add(Restrictions.isNotNull("av.stringValue")).
                        add(Restrictions.isNotNull("av.booleanValue")).
                        add(Restrictions.isNotNull("av.longValue")).
                        add(Restrictions.isNotNull("av.doubleValue")).
                        add(Restrictions.isNotNull("av.dateValue"));
                break;

            case LE:
                result = Restrictions.disjunction().
                        add(Restrictions.le("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.le("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.le("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.le("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.le("av.dateValue",
                        example.getDateValue()));
                break;

            case LIKE:
                // LIKE operator is meaningful for strings only
                result = Restrictions.disjunction().
                        add(Restrictions.like("av.stringValue",
                        example.getStringValue()));
                break;

            case LT:
                result = Restrictions.disjunction().
                        add(Restrictions.lt("av.stringValue",
                        example.getStringValue())).
                        add(Restrictions.lt("av.booleanValue",
                        example.getBooleanValue())).
                        add(Restrictions.lt("av.longValue",
                        example.getLongValue())).
                        add(Restrictions.lt("av.doubleValue",
                        example.getDoubleValue())).
                        add(Restrictions.lt("av.dateValue",
                        example.getDateValue()));
                break;

            default:
        }
        return result;
    }
}