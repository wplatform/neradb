/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.neradb.command.expression;

import com.neradb.dbobject.table.ColumnResolver;
import com.neradb.dbobject.table.TableFilter;
import com.neradb.engine.Session;
import com.neradb.value.Value;
import com.neradb.value.ValueNull;

/**
 * A NOT condition.
 */
public class ConditionNot extends Condition {

    private Expression condition;

    public ConditionNot(Expression condition) {
        this.condition = condition;
    }

    @Override
    public Expression getNotIfPossible(Session session) {
        return condition;
    }

    @Override
    public Value getValue(Session session) {
        Value v = condition.getValue(session);
        if (v == ValueNull.INSTANCE) {
            return v;
        }
        return v.convertTo(Value.BOOLEAN).negate();
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level) {
        condition.mapColumns(resolver, level);
    }

    @Override
    public Expression optimize(Session session) {
        Expression e2 = condition.getNotIfPossible(session);
        if (e2 != null) {
            return e2.optimize(session);
        }
        Expression expr = condition.optimize(session);
        if (expr.isConstant()) {
            Value v = expr.getValue(session);
            if (v == ValueNull.INSTANCE) {
                return ValueExpression.getNull();
            }
            return ValueExpression.get(v.convertTo(Value.BOOLEAN).negate());
        }
        condition = expr;
        return this;
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        condition.setEvaluatable(tableFilter, b);
    }

    @Override
    public String getSQL() {
        return "(NOT " + condition.getSQL() + ")";
    }

    @Override
    public void updateAggregate(Session session) {
        condition.updateAggregate(session);
    }

    @Override
    public void addFilterConditions(TableFilter filter, boolean outerJoin) {
        if (outerJoin) {
            // can not optimize:
            // select * from test t1 left join test t2 on t1.id = t2.id where
            // not t2.id is not null
            // to
            // select * from test t1 left join test t2 on t1.id = t2.id and
            // t2.id is not null
            return;
        }
        super.addFilterConditions(filter, outerJoin);
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        return condition.isEverything(visitor);
    }

    @Override
    public int getCost() {
        return condition.getCost();
    }

}
