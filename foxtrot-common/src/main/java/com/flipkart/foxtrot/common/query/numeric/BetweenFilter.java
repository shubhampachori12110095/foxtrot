package com.flipkart.foxtrot.common.query.numeric;

import com.flipkart.foxtrot.common.query.Filter;
import com.flipkart.foxtrot.common.query.FilterOperator;
import com.flipkart.foxtrot.common.query.FilterVisitor;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.validation.constraints.NotNull;

/**
 * User: Santanu Sinha (santanu.sinha@flipkart.com)
 * Date: 14/03/14
 * Time: 2:10 AM
 */
public class BetweenFilter extends Filter {
    private boolean temporal = false;
    @NotNull
    private Number from;
    @NotNull
    private Number to;

    public BetweenFilter() {
        super(FilterOperator.between);
    }

    public Number getFrom() {
        return from;
    }

    public void setFrom(Number from) {
        this.from = from;
    }

    public Number getTo() {
        return to;
    }

    public void setTo(Number to) {
        this.to = to;
    }

    @Override
    public void accept(FilterVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BetweenFilter that = (BetweenFilter) o;

        if (!from.equals(that.from)) return false;
        if (!to.equals(that.to)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        if(!temporal) {
            result = 31 * result + from.hashCode();
            result = 31 * result + to.hashCode();
        }
        else {
            result = 31 * result + Long.valueOf(from.longValue() / 30000).hashCode();
            result = 31 * result + Long.valueOf(to.longValue()/30000).hashCode();
        }
        return result;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("from", from)
                .append("to", to)
                .toString();
    }

    public boolean isTemporal() {
        return temporal;
    }

    public void setTemporal(boolean temporal) {
        this.temporal = temporal;
    }
}
