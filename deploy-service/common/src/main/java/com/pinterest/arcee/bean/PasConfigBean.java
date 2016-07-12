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

    @JsonProperty("throughput")
    private Integer throughput;
    
    @JsonProperty("metric")
    private String metric;

    @JsonProperty("pas_state")
    private PasState pas_state;

    @JsonProperty("last_updated")
    private Long last_updated;

    @JsonProperty("defined_min_size")
    private Integer defined_min_size;

    @JsonProperty("defined_max_size")
    private Integer defined_max_size;

    public Integer getDefined_min_size() {
        return defined_min_size;
    }

    public void setDefined_min_size(Integer defined_min_size) {
        this.defined_min_size = defined_min_size;
    }

    public Integer getDefined_max_size() {
        return defined_max_size;
    }

    public void setDefined_max_size(Integer defined_max_size) {
        this.defined_max_size = defined_max_size;
    }

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

    public Integer getThroughput() {
        return throughput;
    }

    public void setThroughput(Integer throughput) {
        this.throughput = throughput;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public PasState getPas_state() {
        return pas_state;
    }

    public void setPas_state(PasState pas_state) {
        this.pas_state = pas_state;
    }

    public final static String UPDATE_CLAUSE =
            "group_name=VALUES(group_name)," +
                    "last_updated=VALUES(last_updated)," +
                    "throughput=VALUES(throughput)," +
                    "metric=VALUES(metric)," +
                    "pas_state=VALUES(pas_state)," +
                    "defined_min_size=VALUES(defined_min_size)," +
                    "defined_max_size=VALUES(defined_max_size)";

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("throughput", throughput);
        clause.addColumn("metric", metric);
        clause.addColumn("pas_state", pas_state);
        clause.addColumn("group_name", group_name);
        clause.addColumn("last_updated", last_updated);
        clause.addColumn("defined_min_size", defined_min_size);
        clause.addColumn("defined_max_size", defined_max_size);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
