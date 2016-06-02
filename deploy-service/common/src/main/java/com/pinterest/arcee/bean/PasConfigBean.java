package com.pinterest.arcee.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class PasConfigBean implements Updatable {

    @NotEmpty
    @JsonProperty("group_name")
    private String group_name;

    @NotNull
    @JsonProperty("throughput")
    private int throughput;

    @NotEmpty
    @JsonProperty("metric")
    private String metric;

    @NotEmpty
    @JsonProperty("pas_state")
    private String pas_state;

    private Long last_updated;

    public String getGroup_name() {
        return group_name;
    }

    public Long getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(Long last_updated) {
        this.last_updated = last_updated;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public int getThroughput() {
        return throughput;
    }

    public void setThroughput(int throughput) {
        this.throughput = throughput;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getPas_state() {
        return pas_state;
    }

    public void setPas_state(String pas_state) {
        this.pas_state = pas_state;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("throughput", throughput);
        clause.addColumn("metric", metric);
        clause.addColumn("pas_state", pas_state);
        clause.addColumn("group_name", group_name);
        clause.addColumn("last_updated", last_updated);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
