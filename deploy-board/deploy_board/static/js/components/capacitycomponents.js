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

function getCapacityAlertMessage(isWarning, remainingCapacity, placements, increase) {
    const errorMessage = `Insufficient combined remaining capacity in this cluster/auto scaling group. `;
    const instruction = `You can attach additional placements to the corresponding clutter to increase` +
                        ` total potential capacity at Cluster Configuration -> Advanced Settings.\n`;
    const status = `Combined remaining capacity: ${remainingCapacity}\n` +
                   `Current placement(s): ${JSON.stringify(placements, ['capacity', 'provider_name', 'abstract_name'], 2)}`;

    if (isWarning) {
        return errorMessage + `You can save this configuration but the cluster/ASG might run into capacity issues in the future. ` +
                instruction + `Requested size increase: ${increase}\n` + status
    } else {
        // error
        return errorMessage + `You shouldn't save this configuration because the cluster/ASG will run into capacity issues. ` +
                instruction + `Requested size increase: ${increase}\n` + status;
    }
}

function getCapacityDoubleAlertMessage(isFixed) {
    const errorMessage = isFixed ? `You are increasing the capacity by more than 100%,` : `You are increasing the minSize or maxSize by more than 100%,`;
    const instruction = ` traffic would start routing requests to all hosts and could lead to SR drop.\n` +
                            `We strongly suggest launch small numbers of hosts then more and more until the desired capacity is reached.\n`;
    const context = `More context: During load balancing, Envoy will generally only consider available (healthy or degraded) hosts in an upstream cluster.\n` +
                        `However, if the percentage of available hosts in the cluster becomes too low, Envoy will disregard health status and balance either amongst all hosts or no hosts.\n` +
                        `This is known as the panic threshold. The default panic threshold is 50%.\n`;
    return errorMessage + instruction + context;
}

function getTerminationLimitAlertMessage(isWarning) {
    const message = isWarning ? `The size you manually scaled down plus the number of hosts in terminating status exceed the specified termination limit.` 
    : `The size you manually scaled down is more than the specified termination limit.`
    return message;
}

function getCapacityScaleDownAlertMessage(isFixed, isWarning) {
    const errorMessage = isFixed ? `You are scaling down the capacity by more than ` : `You are scaling down the minSize or maxSize by more than `;
    const percentage = isWarning ? `33%,` : `50%,`;
    const instruction = ` traffic would start routing requests to all hosts and could lead to SR drop.\n` +
                            `We strongly suggest scale down small numbers of hosts then more and more until the desired capacity is reached.\n`;
    const context = `More context: During load balancing, Envoy will generally only consider available (healthy or degraded) hosts in an upstream cluster.\n` +
                        `However, if the percentage of available hosts in the cluster becomes too low, Envoy will disregard health status and balance either amongst all hosts or no hosts.\n` +
                        `This is known as the panic threshold. The default panic threshold is 50%.\n`;
    return errorMessage + percentage + instruction + context;
}

function calculateImbalanceThreshold(totalIncrease, numPlacements) {
    // average increase per placement
    return (totalIncrease / numPlacements).toFixed()
}

function checkImbalance(placements, threshold) {
    var insufficientPlacements = [];
    var showImbalanceWarning = false;
    for (var p of placements) {
        if (p.capacity < threshold) {
            showImbalanceWarning = true;
            insufficientPlacements.push(p)
        }
    }
    return showImbalanceWarning ? `The ASG might have AZ imbalance based on a potential increase of ${threshold} hosts per placement. `+
        `Please pay attention to the following placements:\n` +
        `${JSON.stringify(insufficientPlacements, ['capacity', 'provider_name', 'abstract_name'], 2)}` : '';
}

/**
 * The capacity button. This is shown when autoscaling is disabled or min\max size are the same.
 * In this case, only one capacity box is shown
 */
Vue.component("static-capacity-config", {
    template: `<div>
    <div class="form-group">
        <label for="capacity" class="deployToolTip control-label col-xs-4" title="Number of hosts for this service">
            Capacity
        </label>
        <div class="col-xs-2" >
            <input name="capacity" class="form-control" type="number" min="0" required
                :value="capacity" v-on:change="onCapacityChange($event.target.value)" @keydown.enter.prevent="">
            <div v-model="remainingCapacity">\
                Remaining Capacity: {{remainingCapacity}}\
            </div>\
        </div>
    </div>
    <form-danger v-show="showSizeError" :alert-text="sizeError"></form-danger>
    <form-warning v-show="showSizeWarning" :alert-text="sizeWarning"></form-warning>
    <form-warning v-show="showImbalanceWarning" :alert-text="imbalanceWarning"></form-warning>
    <form-danger v-show="showTerminationError" :alert-text="terminationError"></form-danger>
    </div>`,
    props: {
        originalCapacity: Number,
        remainingCapacity: Number,
        placements: Object,
        terminationLimit: Number,
        terminatingHostCount: Number,
    },
    data: function() {
        return {
            capacity: this.originalCapacity,
            terminationLimit: this.terminationLimit,
            terminatingHostCount: this.terminatingHostCount,
            showSizeError: false,
            showImbalanceWarning: false,
            sizeError: '',
            imbalanceWarning: '',
            showSizeWarning: false,
            sizeWarning: '',
            showTerminationError: false,
            terminationError: '',
        }
    },
    methods: {
        onCapacityChange: function (value) {
            this.capacity = Number(value);
            this.showSizeError = false;
            this.showSizeWarning = false;
            this.showTerminationError = false;
            this.validateSize();
            this.$emit('change', this.capacity );
        },
        validateSize: function () {
            var data = {
                prefetch: {
                    url: '/groups/helloworlddummyservice-server-dev1-yaqin-test/hosts',
                    data: JSON.stringify({"actionType": "TERMINATING"}),
                    success: function (result) {
                        return result;
                    }
                }
            }
            console.log(data);
            const sizeIncrease = this.capacity - this.originalCapacity;
            if (sizeIncrease >= this.remainingCapacity) {
                this.sizeError = getCapacityAlertMessage(false, this.remainingCapacity, this.placements, sizeIncrease);
                this.showSizeError = true;
            } else {
                if (sizeIncrease > this.originalCapacity && this.originalCapacity > 0) {
                    if (this.originalCapacity > 100) {
                        this.showSizeError = true;
                        this.sizeError = getCapacityDoubleAlertMessage(true);
                    } else {
                        this.showSizeWarning = true;
                        this.sizeWarning = getCapacityDoubleAlertMessage(true);
                    }
                } else if (-sizeIncrease > this.capacity && this.capacity > 0) {
                    this.showSizeError = true;
                    this.sizeError = getCapacityScaleDownAlertMessage(true, false);
                } else if (-sizeIncrease * 2 > this.capacity && this.capacity > 0) {
                    this.showSizeWarning = true;
                    this.sizeWarning = getCapacityScaleDownAlertMessage(true, true);
                }
            }

            this.imbalanceWarning = checkImbalance(this.placements, calculateImbalanceThreshold(sizeIncrease, this.placements.length));
            this.showImbalanceWarning = this.imbalanceWarning != '';

            if (this.terminationLimit !== null) {
                if (-sizeIncrease > this.terminationLimit) {
                    this.showTerminationError = true;
                    this.terminationError = getTerminationLimitAlertMessage(false);
                } else if (-sizeIncrease > this.terminationLimit - this.terminatingHostCount) {
                    this.showTerminationError = true;
                    this.terminationError = getTerminationLimitAlertMessage(true);
                }
            } 
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
                    :value="minSize" v-on:change="onMinSizeChange($event.target.value)" @keydown.enter.prevent="" >
            </div>
        </div>
        <div :class="inputBootstrapClass">
            <div class="input-group">
                <span class="input-group-addon">Max Size</span>
                <input name="maxSize" class="form-control" type="number" min="0" required
                    :value="maxSize" v-on:change="onMaxSizeChange($event.target.value)" @keydown.enter.prevent="">
            </div>
        </div>
    </div>
    <form-danger v-show="showSizeError" :alert-text="sizeError"></form-danger>
    <form-warning v-show="showSizeWarning" :alert-text="sizeWarning"></form-warning>
    <form-warning v-show="showImbalanceWarning" :alert-text="imbalanceWarning"></form-warning>
    </div>`,
    props: {
        labelBootstrapClass: {
            type: String,
            default: 'col-xs-4',
        },
        inputBootstrapClass: {
            type: String,
            default: 'col-xs-2',
        },
        labelText: {
            type: String,
            default: 'Number of hosts for this service',
        },
        labelTitle: {
            type: String,
            default: 'Capacity'
        },
        currentSize: Number,
        originalMinSize: Number,
        originalMaxSize: Number,
        remainingCapacity: Number,
        placements: Object,
    },
    data: function() {
        return {
            minSize: this.originalMinSize,
            maxSize: this.originalMaxSize,
            showSizeError: false,
            showSizeWarning: false,
            showImbalanceWarning: false,
            sizeError: '',
            sizeWarning: '',
            imbalanceWarning: '',
        }
    },
    methods: {
        onMinSizeChange: function (value) {
            this.minSize = Number(value);
            if (this.maxSize < this.minSize) {
                this.maxSize = this.minSize;
            }
            this.showSizeError = false;
            this.showSizeWarning = false;
            this.validateSize();
            this.$emit('min-change', this.minSize);
        },
        onMaxSizeChange: function (value) {
            this.maxSize = Number(value);
            if (this.maxSize < this.minSize) {
                this.minSize = this.maxSize;
            }
            this.showSizeError = false;
            this.showSizeWarning = false;
            this.validateSize();
            this.$emit('max-change', this.maxSize);
        },
        validateSize: function() {
            const minIncrease = this.minSize - this.originalMinSize;
            const maxIncrease = this.maxSize - this.originalMaxSize;

            if (minIncrease >= this.remainingCapacity) {
                this.sizeError = getCapacityAlertMessage(false, this.remainingCapacity, this.placements, minIncrease);
                this.showSizeError = true;
            } else {
                this.showSizeError = false;
            }

            if (!this.showSizeError && maxIncrease >= this.remainingCapacity) {
                this.sizeWarning = getCapacityAlertMessage(true, this.remainingCapacity, this.placements, maxIncrease);
                this.showSizeWarning = true;
            } else {
                if (maxIncrease > this.originalMaxSize && this.originalMaxSize > 0 || minIncrease > this.originalMinSize && this.originalMinSize > 0) {
                    if (this.originalMaxSize > 100) {
                        this.showSizeError = true;
                        this.sizeError = getCapacityDoubleAlertMessage(false);
                    } else {
                        this.showSizeWarning = true;
                        this.sizeWarning = getCapacityDoubleAlertMessage(false);
                    }
                } else if (-minIncrease > this.minSize && this.minSize > 0 || -maxIncrease > this.maxSize && this.maxSize > 0) {
                    this.showSizeError = true;
                    this.sizeError = getCapacityScaleDownAlertMessage(false, false);
                } else if (-minIncrease * 2 > this.minSize && this.minSize > 0 || -maxIncrease * 2 > this.maxSize && this.maxSize > 0) {
                    this.showSizeWarning = true;
                    this.sizeWarning = getCapacityScaleDownAlertMessage(false, true);
                }
            }

            const avgSizeIncreasePerPlacement = calculateImbalanceThreshold(this.maxSize - this.currentSize, this.placements.length);
            this.imbalanceWarning = checkImbalance(this.placements, avgSizeIncreasePerPlacement);
            this.showImbalanceWarning = this.imbalanceWarning != '';
        },
    },
    attached: function() {
        this.validateSize();
    }
});
