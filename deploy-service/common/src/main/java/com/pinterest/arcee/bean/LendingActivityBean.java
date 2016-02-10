package com.pinterest.arcee.bean;


import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

/*
CREATE TABLE IF NOT EXISTS lending_activities (
    id          VARCHAR(32)   NOT NULL,
    group_name  VARCHAR(32)   NOT NULL,
    actity_type VARCHAR(10)   NOT NULL,
    reason      VARCHAR(1024) NOT NULL,
    update_time  BIGINT       NOT NULL,
    PRIMARY KEY (id)
);
 */
public class LendingActivityBean implements Updatable {
    private String id;

    private String group_name;

    private String activity_type;

    private String reason;

    private Long update_time;

    public void setId(String id) { this.id = id; }

    public String getId() { return this.id; }

    public void setGroup_name(String group_name) { this.group_name = group_name; }

    public String getGroup_name() { return group_name; }

    public void setActivity_type(String activity_type) { this.activity_type = activity_type; }

    public String getActivity_type() { return activity_type;}

    public void setReason(String reason) { this.reason = reason; }

    public String getReason() { return reason; }

    public void setUpdate_time(Long update_time) { this.update_time = update_time; }

    public Long getUpdate_time() { return update_time; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("group_name", group_name);
        clause.addColumn("activity_type", activity_type);
        clause.addColumn("reason", reason);
        clause.addColumn("update_time", update_time);
        return clause;
    }
}
