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
    private Integer throughput;

    @NotEmpty
    @JsonProperty("metric")
    private String metric;

    @JsonProperty("pas_state")
    private PasState pas_state;

    @JsonProperty("min_size")
    private Integer min_size;

    @JsonProperty("max_size")
    private Integer max_size;

    @JsonProperty("cool_down")
    private Integer cool_down;

    @JsonProperty("last_updated")
    private Long last_updated;


    public Integer getMin_size() {
        return min_size;
    }

    public void setMin_size(Integer min_size) {
        this.min_size = min_size;
    }

    public Integer getMax_size() {
        return max_size;
    }

    public void setMax_size(Integer max_size) {
        this.max_size = max_size;
    }

    public Integer getCool_down() {
        return cool_down;
    }

    public void setCool_down(Integer cool_down) {
        this.cool_down = cool_down;
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
                    "min_size=VALUES(min_size)," +
                    "max_size=VALUES(max_size)," +
                    "cool_down=VALUES(cool_down)";

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("throughput", throughput);
        clause.addColumn("metric", metric);
        clause.addColumn("pas_state", pas_state);
        clause.addColumn("group_name", group_name);
        clause.addColumn("last_updated", last_updated);
        clause.addColumn("min_size", min_size);
        clause.addColumn("max_size", max_size);
        clause.addColumn("cool_down", cool_down);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
