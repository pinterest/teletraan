/**
 * Cluster Capacity Setting Components
 */


/**
 * Delete Cluster button on the side panel
 */
Vue.component('delete-cluster', {
    template: '<div v-show="showdelete">\
    <side-button-modal-confirm confirmDiaglog="#deleteClusterDialogId" \
        text="Delete Cluster" title="Delete Cluster" styleclass="glyphicon glyphicon-remove-sign">\
    </side-button-modal-confirm>\
    <modal title="Delete Cluster Confirm" id="deleteClusterDialogId" v-on:input="clickDialog">\
        <div slot="body">\
            Are you really sure to <strong>DELETE</strong> cluster?<br>\
            If you DELETE the cluster, <strong>ALL HOSTS</strong> in this cluster will be <strong>TERMINATED</strong>.\
        </div>\
    </modal></div>',
    props: ['showdelete'],
    methods: {
        clickDialog: function (value) {
            this.$emit('input', value);
        }
    }
});

/**
 * Clone Cluster button on the side panel
 */
Vue.component('clone-cluster', {
    template: '<div v-show="showclone">\
    <side-button-modal-confirm confirmDiaglog="#cloneClusterDialogId" \
        text="Clone Cluster" title="Clone Cluster" styleclass="glyphicon glyphicon-file">\
    </side-button-modal-confirm>\
    <modal title="Clone Cluster Confirm" id="cloneClusterDialogId" v-on:input="clickDialog">\
        <div slot="body">\
           The fleet size (capacity) will not be cloned, initially the capacity is 0, ASG size is 0. \
           <div class="panel-body">\
                <div class="form-group">\
                    <div class="col-xs-10" style="padding: 6px;">\
                        <div class="input-group" >\
                            <span class="input-group-addon">Environment Name</span>\
                            <input class="form-control" v-on:change="updateNewEnvironment($event.target.value)"></input>\
                        </div>\
                    </div>\
                </div>\
                <div class="form-group">\
                    <div class="col-xs-10" style="padding: 6px;">\
                        <div class="input-group" >\
                            <span class="input-group-addon">Stage Name</span>\
                            <input class="form-control" v-on:change="updateNewStage($event.target.value)"></input>\
                        </div>\
                    </div>\
                </div>\
             </div>\
        </div>\
    </modal>\
    </div>',
    props: ['showclone'],
    methods: {
        clickDialog: function (value) {
            if(value == true) {
                var new_environment = this.new_environment;
                var new_stage = this.new_stage;
                this.$emit('input', new_environment, new_stage);
            }
        },
        updateNewEnvironment: function (value) {
            this.new_environment = value;
        },
        updateNewStage: function (value) {
            this.new_stage = value;
        }
    }
});


/**
 * Cluster Replace buttons. Depending on the cluster state, it can show different buttons:
 *   Cluster State:
 *      Normal: Replace button
 *      Replace: Cancel Button and Pause Button
 *      Pause: Pause Button and Resume Button
 */
Vue.component('replace-cluster', {
    template: '<div>\
    <side-button-modal-confirm v-show="showreplace" confirmDiaglog="#replaceClusterDialogId" \
        text="Replace Cluster" title="Click to replace cluster with new launch configuration" styleclass="glyphicon glyphicon-random">\
    </side-button-modal-confirm>\
    <modal title="Replace Cluster Confirm" id="replaceClusterDialogId" v-on:input="replaceClickDialog">\
        <div slot="body">\
            Are you sure to <strong>REPLACE</strong> every host in this cluster?\
        </div>\
    </modal>\
    <side-button-modal-confirm v-show="showpause" confirmDiaglog="#pauseClusterDialogId" \
        text="Pause Replacement" title="Click to pause ongoing cluster replacement" styleclass="glyphicon glyphicon-pause">\
    </side-button-modal-confirm>\
    <modal title="Pause Ongoing Cluster Replacement Confirm" id="pauseClusterDialogId" v-on:input="pauseClickDialog">\
        <div slot="body">\
          Are you sure to pause cluster replacement?\
        </div>\
    </modal>\
    <side-button-modal-confirm v-show="showcancel" confirmDiaglog="#cancelClusterDialogId" \
        text="Cancel Replacement" title="Click to cancel ongoing cluster replacement" styleclass="glyphicon glyphicon-step-backward">\
    </side-button-modal-confirm>\
    <modal title="Cancel Ongoing Cluster Replacement Confirm" id="cancelClusterDialogId" v-on:input="cancelClickDialog">\
        <div slot="body">\
          Are you sure to cancel ongoing cluster replacement?\
        </div>\
    </modal>\
    <side-button-modal-confirm v-show="showresume" confirmDiaglog="#resumeClusterDialogId" \
        text="Resume Replacement" title="Click to resume ongoing cluster replacement" styleclass="glyphicon glyphicon-play">\
    </side-button-modal-confirm>\
    <modal title="Resume Ongoing Cluster Replacement Confirm" id="resumeClusterDialogId" v-on:input="resumeClickDialog">\
        <div slot="body">\
          Are you sure to resume cluster replacement?\
        </div>\
    </modal>\
    </div>',
    props: ['clusterstate'],
    data: function () {
        return {
            showreplace: this.clusterstate === "NORMAL",
            showcancel: this.clusterstate === "REPLACE" || this.clusterstate === "PAUSE",
            showpause: this.clusterstate === "REPLACE",
            showresume: this.clusterstate === "PAUSE",
        }
    },
    methods: {
        replaceClickDialog: function (value) {
            this.$emit('input', { action: 'replace', value });
        },
        pauseClickDialog: function (value) {
            this.$emit('input', { action: 'pause', value });
        },
        cancelClickDialog: function (value) {
            this.$emit('input', { action: 'cancel', value });
        },
        resumeClickDialog: function (value) {
            this.$emit('input', { action: 'resume', value });
        },
    }
});

/**
 * This component represents the heading of the cluster capacity panel where replace buttons are shown
 */
Vue.component("cluster-capacity-panel-heading", {
    template: '<div class="panel-heading clearfix">\
    <h4 class="panel-title pull-left pointer-cursor">\
        <a v-on:click="toggle" data-toggle="collapse" v-bind:data-target="target">\
            <span v-bind:class="getclass(collapse)">\
            </span>\
            {{ title }}\
        </a>\
    </h4>\
</div>',
    props: ['title', 'target', 'initcollapse'],
    data: function () {
        return {
            collapse: this.initcollapse,
        }
    },
    methods: {
        getclass: function (collapse) {
            if (collapse)
                return "glyphicon glyphicon-chevron-right"
            else
                return "glyphicon glyphicon-chevron-down"
        },
        toggle: function () {
            this.collapse = !this.collapse
        }
    }
});

/**
 * Warning banner when an underlying replacement is ongoing
 */
Vue.component('in-rolling-alert', {
    template: '\
    <div class="alert alert-info">\
        <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
        <strong>Warning!</strong> This environment stage has scheduled cluster rolling upgrade.\
        You cannot update this page unless you <a v-bind:href="actionlink">cancel cluster replacement.</a>\
    </div>',
    props:['actionlink']
});


/**
 * The capacity button. This is shown when autoscaling is disabled or min\max size are the same.
 * In this case, only one capacity box is shown
 */
Vue.component("static-capacity-config", {
    template: '<div class="form-group">\
    <label for="capacity" class="deployToolTip control-label col-xs-4" title="Number of hosts for this service">\
        Capacity\
    </label>\
    <div class="col-xs-2" >\
    <input class="form-control" v-bind:value="capacity" v-on:change="updateValue($event.target.value)" @keydown.enter.prevent="" type="number" min="0"></input>\
    </div></div>',
    props: ['capacity'],
    methods: {
        updateValue: function (value) {
            this.$emit("change", value)
        }
    }
});

/**
 * The capacity button groups. This is shown when min and max size are different
 * In this case, both min size and max size buttons are shown
 */
Vue.component("asg-capacity-config", {
    template: `<div>
    <div class="form-group">
        <label for="capacity" data-toggle="tooltip" class="deployToolTip control-label" :class="labelBootstrapClass" :title="labelText">
            {{ labelTitle }}
        </label>
        <div :class="inputBootstrapClass">
            <div class="input-group">
                <span class="input-group-addon">Min Size</span>
                <input name="minSize" class="form-control" type="number" min="0" required
                    v-model="minSize" @keydown.enter.prevent="" >
            </div>
        </div>
        <div :class="inputBootstrapClass">
            <div class="input-group">
                <span class="input-group-addon">Max Size</span>
                <input name="maxSize" class="form-control" type="number" min="0" required
                    v-model="maxSize" @keydown.enter.prevent="">
            </div>
        </div>
    </div>
    <form-warning v-show="showSizeWarning" :warning-text="sizeWarning"></form-warning>
    <form-error v-show="showSizeError" :error-text="sizeError"></form-error>
    </div>`,
    props: {
        labelBootstrapClass: 'col-xs-4',
        inputBootstrapClass: 'col-xs-2',
        labelText: 'Number of hosts for this service',
        labelTitle: 'Capacity',
        originalMinSize: 0,
        originalMaxSize: 0,
        remainingCapacity: Infinity,
        placements: {},
    },
    data: function() {
        return {
            minSize: this.originalMinSize,
            maxSize: this.originalMaxSize,
            showSizeError: false,
            showSizeWarning: false,
            sizeError: '',
            sizeWarning: '',
        }
    },
    watch: {
        minSize: function (value) {
            this.minSize = value;
            if (this.maxSize < this.minSize) {
                this.maxSize = this.minSize;
            }
            this.validateSize();
        },
        maxSize: function (value, oldValue) {
            this.maxSize = value;
            if (this.maxSize < this.minSize) {
                this.minSize = this.maxSize;
            }
            this.validateSize();
        },
    },
    methods: {
        validateSize: function() {
            const minIncrease = this.minSize - this.originalMinSize;
            const maxIncrease = this.maxSize - this.originalMaxSize;
            const instruction = `You can attach additional placements to the corresponding clutter to increase ` +
                                `total potential capacity at Cluster Configuration -> Advanced Settings.\n`;
            const status = `Combined remaining capacity: ${this.remainingCapacity}\n` +
                           `Current placement(s): ${JSON.stringify(this.placements, null, 2)}`;
            if (minIncrease >= this.remainingCapacity) {
                this.sizeError = `Insufficient combined remaining capacity in this cluster/auto scaling group. ` +
                    `You shouldn't save this configuration because the cluster/ASG will run into capacity issues. ` +
                    instruction + `Requested size increase: ${minIncrease}\n` + status

                this.showSizeError = true;
            } else {
                this.showSizeError = false;
            }

            if(!this.showSizeError && maxIncrease >= this.remainingCapacity){
                this.sizeWarning = `Insufficient combined remaining capacity in this cluster/auto scaling group. ` +
                    `You can save this configuration but the cluster/ASG might run into capacity issues in the future. ` +
                    instruction + `Requested size increase: ${maxIncrease}\n` + status;
                this.showSizeWarning = true;
            } else {
                this.showSizeWarning = false;
            }
        },
    }
});
