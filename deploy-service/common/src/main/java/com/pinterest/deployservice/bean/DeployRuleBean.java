package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.io.Serializable;

/**
 * Keep the bean and table in sync
 * CREATE TABLE deploy_rules (
 * rule_id VARCHAR(22)         NOT NULL,
 * condition_key  VARCHAR(64)  NOT NULL,
 * condition_value VARCHAR(256),
 * max_parallel    INT                 NOT NULL,
 * PRIMARY KEY   (rule_id)
 * )
 */
public class DeployRuleBean implements Updatable, Serializable {
    @JsonProperty("id")
    private String rule_id;

    @JsonProperty("conditionKey")
    private String condition_key;

    @JsonProperty("conditionValue")
    private String condition_value;

    @JsonProperty("maxParallel")
    private long max_parallel;

    public String getRule_id() {
        return rule_id;
    }

    public void setRule_id(String rule_id) {
        this.rule_id = rule_id;
    }

    public String getCondition_key() {
        return condition_key;
    }

    public void setCondition_key(String condition_key) {
        this.condition_key = condition_key;
    }

    public String getCondition_value() {
        return condition_value;
    }

    public void setCondition_value(String condition_value) {
        this.condition_value = condition_value;
    }

    public long getMax_parallel() {
        return max_parallel;
    }

    public void setMax_parallel(long max_parallel) {
        this.max_parallel = max_parallel;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("rule_id", rule_id);
        clause.addColumn("condition_key", condition_key);
        clause.addColumn("condition_value", condition_value);
        clause.addColumn("max_parallel", max_parallel);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
