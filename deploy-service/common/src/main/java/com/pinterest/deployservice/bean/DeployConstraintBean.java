package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * CREATE TABLE deploy_constraints (
 * constraint_id     VARCHAR(22)         NOT NULL,
 * constraint_key    VARCHAR(64)         NOT NULL
 * max_parallel      BIGINT              NOT NULL,
 * constraint_type   VARCHAR(32)         NOT NULL,
 * state             VARCHAR(32)         NOT NULL,
 * start_date        BIGINT              NOT NULL,
 * last_update       BIGINT,
 * PRIMARY KEY   (constraint_id)
 * )
 */
public class DeployConstraintBean implements Updatable {
    @JsonProperty("id")
    private String constraint_id;

    @JsonProperty("constraintKey")
    private String constraint_key;

    @JsonProperty("maxParallel")
    private Long max_parallel;

    @JsonProperty("constraintType")
    private DeployConstraintType constraint_type;

    @JsonProperty("state")
    private TagSyncState state;

    @JsonProperty("startDate")
    private Long start_date;

    @JsonProperty("lastUpdate")
    private Long last_update;

    public String getConstraint_id() {
        return constraint_id;
    }

    public void setConstraint_id(String constraint_id) {
        this.constraint_id = constraint_id;
    }

    public String getConstraint_key() {
        return constraint_key;
    }

    public void setConstraint_key(String constraint_key) {
        this.constraint_key = constraint_key;
    }

    public Long getMax_parallel() {
        return max_parallel;
    }

    public void setMax_parallel(Long max_parallel) {
        this.max_parallel = max_parallel;
    }

    public DeployConstraintType getConstraint_type() {
        return constraint_type;
    }

    public void setConstraint_type(DeployConstraintType constraint_type) {
        this.constraint_type = constraint_type;
    }

    public TagSyncState getState() {
        return state;
    }

    public void setState(TagSyncState state) {
        this.state = state;
    }

    public Long getStart_date() {
        return start_date;
    }

    public void setStart_date(Long start_date) {
        this.start_date = start_date;
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("constraint_id", constraint_id);
        clause.addColumn("constraint_key", constraint_key);
        clause.addColumn("max_parallel", max_parallel);
        clause.addColumn("constraint_type", constraint_type);
        clause.addColumn("state", state);
        clause.addColumn("start_date", start_date);
        clause.addColumn("last_update", last_update);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
