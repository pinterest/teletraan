package com.pinterest.deployservice.events;

import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.teletraan.universal.events.AppEvent;

public class BuildEvent extends AppEvent {
    private final BuildBean buildBean;
    private final String action;

    public BuildEvent(Object source, BuildBean buildBean, String action) {
        super(source);
        this.buildBean = buildBean;
        this.action = action;
    }

    public BuildBean getBuildBean() {
        return buildBean;
    }

    public String getAction() {
        return action;
    }
}
