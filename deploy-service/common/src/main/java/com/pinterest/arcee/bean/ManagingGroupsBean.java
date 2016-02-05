package com.pinterest.arcee.bean;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

/**
 * CREATE TABLE IF NOT EXISTS managing_groups {
 * group_name          VARCHAR(32) NOT NULL,
 * max_lending_size    INT         NOT NULL,
 * lending_priority    VARCHAR(16) NOT NULL,
 * batch_size          INT         NOT NULL,
 * cool_down           INT         NOT NULL,
 * lent_size           INT         NOT NULL,
 * last_activity_time  BIGINT      NOT NULL,
 * PRIMARY KEY (group_name)
 * }
 */
public class ManagingGroupsBean implements Updatable {
    private String group_name;

    private Integer max_lending_size;

    private String lending_priority;

    private Integer batch_size;

    private Integer cool_down;

    private Integer lent_size;

    private Long last_activity_time;

    public String getGroup_name() { return group_name; }

    public void setGroup_name(String group_name) { this.group_name = group_name; }

    public Integer getMax_lending_size() { return max_lending_size; }

    public void setMax_lending_size(Integer max_lending_size) { this.max_lending_size = max_lending_size; }

    public String getLending_priority() { return lending_priority; }

    public void setLending_priority(String lending_priority) { this.lending_priority = lending_priority; }

    public Integer getBatch_size() { return batch_size; }

    public void setBatch_size(Integer batch_size) { this.batch_size = batch_size; }

    public Integer getCool_down() { return cool_down; }

    public void setCool_down(Integer cool_down) { this.cool_down = cool_down; }

    public Integer getLent_size() { return lent_size; }

    public void setLent_size(Integer lent_size) { this.lent_size = lent_size; }

    public Long getLast_activity_time() { return last_activity_time; }

    public void setLast_activity_time(Long last_activity_time) {  this.last_activity_time = last_activity_time; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("group_name", group_name);
        clause.addColumn("max_lending_size", max_lending_size);
        clause.addColumn("lending_priority", lending_priority);
        clause.addColumn("batch_size", batch_size);
        clause.addColumn("cool_down", cool_down);
        clause.addColumn("lent_size", lent_size);
        clause.addColumn("last_activity_time", last_activity_time);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
            "group_name=VALUES(group_name)," +
            "max_lending_size=VALUES(max_lending_size)," +
            "lending_priority=VALUES(lending_priority)," +
            "batch_size=VALUES(batch_size)," +
            "cool_down=VALUES(cool_down)," +
            "lent_size=VALUES(lent_size)," +
            "last_activity_time=VALUES(last_activity_time)";
}
