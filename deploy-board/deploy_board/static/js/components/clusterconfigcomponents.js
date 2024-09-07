Vue.component('cloudprovider-select', {
    template: '<div>\
  <label-select label="Cloud Provider" title="Provider" \
  v-bind:value="value" v-bind:selectoptions="cloudproviders" v-on:input="updateValue(value)"></label-select></div>',
    props: ['cloudproviders', 'value'],
    methods: {
        updateValue: function (value) {
            this.$emit('input', value)
        }
    }
});

Vue.component('accounts-select', {
    template: '<div v-if="accounts">\
  <label-select label="Account" title="Account" \
      v-bind:value="value" \
      v-bind:selectoptions="accounts" \
      :disabled="disabled" \
      v-on:input="updateAccountValue"></label-select></div>',
    props: ['accounts', 'value', 'disabled'],
    methods: {
        updateAccountValue: function (value) {
            this.$emit('accountchange', value);
        }
    }
});

Vue.component('cell-select', {
    template: '<div>\
  <label-select label="Cell" title="Cell" \
  v-bind:value="value" v-bind:selectoptions="cells" v-on:input="updateCellValue"></label-select></div>',
    props: ['cells', 'value'],
    methods: {
        updateValue: function (value) {
            this.$emit('input', value);
        },
        updateCellValue: function (value) {
            this.$emit('cellchange', value);
        }
    }
});

Vue.component('arch-select', {
    template: '<div>\
  <label-select label="Arch" title="Arch" \
  v-bind:value="value" v-bind:selectoptions="arches" v-on:input="updateArchValue"></label-select></div>',
    props: ['arches', 'value'],
    methods: {
        updateValue: function (value) {
            this.$emit('input', value);
        },
        updateArchValue: function (value) {
            this.$emit('archchange', value);
        }
    }
});

Vue.component('baseimage-select', {
    template: `
    <div>
        <div class="form-group">
            <label-select2 col-class="col-xs-3" show-help="true" label="Image Name" title="Abstract Name"
                :options="imageNames" :selected="selectedImageName"
                @input="$emit('image-name-change', $event)" @help-clicked="helpClick">
            </label-select2>
            <label-select2 col-class="col-xs-3" show-help="true" label="Image" title="Provider Name"
                :disabled="!pinImage" :options="baseImages" :selected="selectedBaseImage"
                @input="$emit('base-image-change', $event)" @help-clicked="helpClick">
            </label-select2>
            <div class="col-xs-2">
                <base-checkbox :checked="pinImage" :enabled="pinImageEnabled"
                    @input="pinImageClick"></base-checkbox>
                <label for="pinImageCB" title="Check the box to select AMI manually. If unchecked, AMI will be auto-updated. If there is no Golden AMI, then only manual selection is supported.">Opt out of Golden AMI</label>
            </div>
        </div>
        <form-warning v-show="showWarning" :alert-text="warningText"></form-warning>
    </div>`,
    model: {
        prop: 'pinImage',
    },
    data: function () {
        return {
            showWarning: false,
            warningText: '',
        }
    },
    props: ['imageNames', 'baseImages', 'selectedImageName', 'selectedBaseImage', 'pinImage', 'pinImageEnabled', 'accountOwnerId'],
    methods: {
        helpClick: function () {
            this.$emit('help-clicked')
        },
        tryShowWarning: function (baseImageId, pinImage, accountOwnerId) {
            if (!pinImage) {
                this.showWarning = false;
            } else {
                const baseImage = this.baseImages.find(i => i.value === baseImageId);
                const warnings = this.createWarningMessages(baseImage, accountOwnerId);
                this.showWarning = warnings.length > 0;
                this.warningText = '\n' + warnings.map(
                    (item, index) => `${index + 1}. ${item}`
                ).join('\n');
            }
        },
        pinImageClick: function (pin) {
            this.$emit('input', pin);
            if (pin) {
                this.tryShowWarning(this.selectedBaseImage, pin);
            } else {
                this.showWarning = false;
            }
        },

        createWarningMessages: function(baseImage, accountOwnerId) {
            const messages = [];
            const oneDay = 1000 * 60 * 60 * 24;
            const imageCreationDate = new Date(baseImage.publishDate);
            let age = Math.round((Date.now() - imageCreationDate) / oneDay);
            if (age > 180) {
                messages.push(
                    `The base image configured is over 180 days old (${age} days). ` +
                    "Please consider update or opt-in Auto Update (Unpin image)."
                );
            }

            const sharedImagesCreationDate = new Date("2024-03-15");
            const primaryAccount = '998131032990';
            if (accountOwnerId && accountOwnerId !== primaryAccount
                && imageCreationDate < sharedImagesCreationDate) {
                const options = { day: 'numeric', month: 'long', year: 'numeric' };
                const formattedDate = sharedImagesCreationDate.toLocaleDateString('en-GB', options);
                messages.push(
                    "The base image has configured in primary account and may not works in " +
                    `the sub account, please use base images after ${formattedDate} ` +
                    "for sub accounts.");
            }
            return messages;
        }
    },
    watch: {
        selectedBaseImage: function (baseImageId) {
            this.tryShowWarning(baseImageId, this.pinImage, this.accountOwnerId);
        },
        accountOwnerId: function (newAccountOwnerId) {
            this.tryShowWarning(this.selectedBaseImage, this.pinImage, newAccountOwnerId);
        }
    },
    mounted() {
        this.tryShowWarning(this.selectedBaseImage, this.pinImage, this.accountOwnerId);
    }
});

Vue.component('base-image-help', {
    template: '<help-table v-bind:headers="baseImageHelpHeaders" v-bind:data="data" v-bind:keys="keys" ></help-table>',
    props: ['data'],
    data: function () {
        return {
            baseImageHelpHeaders: [{ name: 'Publish Date', headerClass: 'col-sm-2' },
            { name: 'Name', headerClass: 'col-sm-1' },
            { name: 'Image', headerClass: 'col-sm-1' },
            { name: 'Qualified', headerClass: 'col-sm-1' },
            { name: 'Description', headerClass: 'col-sm-4' },
            { name: 'Acceptance', headerClass: 'col-sm-4' }],
            keys: ['publish_date', 'abstract_name', 'provider_name', 'qualified', 'description', 'acceptance']
        }
    },
}
)



Vue.component('add-config-button', {
    template: '<div class="pull-right">\
    <button type="button" class="deployToolTip btn btn-default btn-sm"\
        data-toggle="modal" v-bind:data-target="target">\
            <span class="glyphicon glyphicon-plus"></span>{{text}}\
        </button>\
    </div>',
    props: ['text', 'target'],
});

Vue.component('data-config-field', {
    template: '<div class="form-group">\
        <label for="properties" class="control-label col-xs-3">{{name}}</label>\
        <div class="col-xs-6">\
            <div class="input-group col-xs-12">\
            <input class="form-control" type="text" v-bind:value="value" v-on:input="change(name,$event.target.value)" v-bind:readonly="readonly ? true : false"></input>\
            <span class="input-group-btn"><button v-if="name==\'pinfo_environment\'" type="button" data-toggle="tooltip" title="" class="deployToolTip btn btn-default" data-original-title="In Teletraan production, please use set the pinfo_environment to &quot;prod&quot; for cmp_base clusters"><span class="glyphicon glyphicon-question-sign"></span></button></span>\
            </div>\
        </div>\
        <div class="col-xs-3">\
            <button type="button" class="delete_button btn btn-default" v-on:click="deleteConfig(name)" v-bind:disabled="readonly ? true : false">Delete</button>\
        </div>\
    </div>',
    props: ['name', 'value', 'readonly'],
    methods: {
        change: function (name, value) {
            this.$emit("change", { name: name, value: value })
        },
        deleteConfig: function (name) {
            this.$emit('deleteConfig', name)
        }
    }
});

Vue.component('aws-user-data', {
    template: '<div class="form-group">\
    <data-config-field v-for="data in alluserdata" v-bind:name="data.name" v-bind:value="data.value" v-bind:readonly="data.readonly" v-bind:inadvanced="inadvanced"\
     v-show="shouldShow(data.name)" v-on:change="change" v-on:deleteConfig="deleteConfig"></data-config-field>\
     <label for="properties" class="control-label col-xs-3 g-0" style="padding-top: 0;">Puppet profile</label>\
     <div class="col-xs-6">\
        <a v-if="puppetFileUrl" v-bind:href="puppetFileUrl" target="_blank">{{ puppetFileUrl }}</a>\
        <span v-else>Unable to find matching Puppet Profile from User Data</span>\
     </div>\
  </div>',
    props: ['alluserdata', 'inadvanced', 'showcmpgroup', 'puppetrepository', 'hierapaths'],
    computed: {
        puppetFileUrl: function() {
            if (!this.hierapaths || !this.puppetrepository) {
                return;
            }
            const userDataObj = this.alluserdata.reduce((acc, entry) => {
                acc[entry.name] = entry.value;
                return acc;
            }, {});

            // hide link for cmp_base profiles
            if (userDataObj['pinfo_role'] === 'cmp_base') {
                return ' ';
            }
            const hieraPaths = this.hierapaths.trim().split("\n").reduce(
                (acc, line) => acc.set(line, [
                    ...new Set(line.matchAll(/(?<=%{::)[a-z0-9_]+(?=})/gi).map(val => val[0]))
                ]),
                new Map()
            );

            const matchingPuppetPath = hieraPaths.keys().find(line =>
                hieraPaths.get(line).every(variable => !!userDataObj[variable])
            );
            const variables = hieraPaths.get(matchingPuppetPath);

            if (!matchingPuppetPath) {
                return;
            }

            const replacedHieraPath = variables.reduce((acc, variable) =>
                acc.replace(`%{::${variable}}`, userDataObj[variable]),
                matchingPuppetPath
            );

            return `${this.puppetrepository}/${replacedHieraPath}.yaml`;
        }
    },
    methods: {
        change: function (value) {
            this.$emit("change", value)
        },
        deleteConfig: function (name) {
            this.$emit('deleteconfig', name)
        },
        shouldShow: function (name) {
            if (this.inadvanced) {
                return this.showcmpgroup ? true : name != "cmp_group"
            }
            else {
                return !name.startsWith("pinfo_") && name != "cmp_group"
            }
        },
    }
});

Vue.component('add-config-button', {
    template: '<button type="button" class="deployToolTip btn btn-default btn-sm" data-toggle="modal" v-bind:data-target="target">\
            <span class="glyphicon glyphicon-plus"></span> Add Config\
        </button>',
    props: ['target']
});

Vue.component("aws-config-modal", {
    template:
        '<div class="modal fade" v-bind:id="id" tabindex="-1" role="dialog"\
     aria-labelledby="newEntryModalLabel" aria-hidden="true">\
    <div class="modal-dialog modal-lg">\
        <div class="modal-content">\
            <form id="newEntryFormId" class="form-horizontal" method="post" role="form">\
                <div class="modal-header">\
                    <button type="button" class="close" data-dismiss="modal"><span\
                            aria-hidden="true">&times;</span><span class="sr-only">Close</span>\
                    </button>\
                    <h4 class="modal-title" id="newEnvModalLabel">Add a new user data config</h4>\
                </div>\
                <div class="modal-body">\
                    <div class="form-group">\
                        <label for="newEntryName" class="col-md-2 control-label">Name</label>\
                        <div class="col-md-10">\
                        <select class="form-control" v-on:change="updateValue($event.target.value)" v-bind:value="selectedValue">\
                            <option v-for="option in options" v-bind:value="option.name" >{{option.text}}</option>\
                        </select>\
                        </div>\
                    </div>\
                    <div class="form-group">\
                        <div class="col-md-9"></div>\
                        <input type="checkbox" class="checkToCustomize" v-model="useCustomizedName" v-on:click="toggleCustomizedName($event.target.checked)"> Check to customize name</input>\
                        <div class="checkToCustomizeKey" v-show="useCustomizedName">\
                            <label for="newCustomizedName" class="col-md-2 control-label">Name</label>\
                            <div class="col-md-10">\
                                <input type="text" class="form-control"placeholder="name..." v-on:keyup="checkReadOnlyValue" v-model="customizedName">\
                                <div input type="text" for="errorMessage" class="text" style="text-align:left; color:red" id="errorDisplay" v-bind:readonly="false"\
                                    v-show="shouldShowError">{{errorMessage}}</label></div>\
                            </div>\
                        </div>\
                    </div>\
                    <div class="form-group">\
                        <label for="newEntryValue" class="col-md-2 control-label">Value</label>\
                        <div class="col-md-10">\
                            <input type="text" class="form-control" placeholder="value..." v-model="selectedOptionValue"></input>\
                        </div>\
                    </div>\
                </div>\
                <div class="modal-footer">\
                    <button type="button" class="btn btn-primary" data-dismiss="modal" v-on:click="addConfig" id="disableConfigButton">Add</button>\
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                </div>\
            </form></div></div></div>',
    props: ["options", "id"],
    data: function () {
        return {
            useCustomizedName: false,
            shouldShowError: false,
            errorMessage: "",
            selectedOption: this.options[0],
            selectedOptionValue: this.options[0].default,
            selectedValue: this.options[0].name,
            customizedName: ""
        };
    },
    methods: {
        updateValue: function (value) {
            this.selectedValue = value;
            if (value === "") {
                this.selectedOptionValue = "";
            } else {
                this.selectedOption = this.options.filter(function (item) {
                    return item.name === value;
                })[0];
                this.selectedOptionValue = this.selectedOption.default;
            }
        },
        addConfig: function () {
            if (this.useCustomizedName) {
                if (
                    this.customizedName != "spiffe_id" &&
                    this.customizedName != "nimbus_id" &&
                    this.customizedName != "assign_public_ip"
                ) {
                    this.$emit("click", {
                        name: this.customizedName,
                        value: this.selectedOptionValue.trim()
                    });
                } else {
                    document.getElementById(
                        "disableConfigButton"
                    ).disabled = true;
                }
            } else {
                this.$emit("click", {
                    name: this.selectedOption.name,
                    value: this.selectedOptionValue.trim()
                });
            }
        },
        checkReadOnlyValue: function () {
            if (this.useCustomizedName) {
                if (this.customizedName === "spiffe_id" ||
                    this.customizedName === "nimbus_id" ||
                    this.customizedName === "assign_public_ip"
                ) {
                    document.getElementById(
                        "disableConfigButton"
                    ).disabled = true;
                    this.shouldShowError = true;
                    if (["spiffe_id", "nimbus_id"].indexOf(this.customizedName) >= 0) {
                        this.errorMessage =
                            this.customizedName + " cannot be added. it will be auto generated";
                    } else {
                        this.errorMessage =
                            this.customizedName + " cannot be added. we only allow assign_public_ip checkbox to modify the value";
                    }
                } else {
                    document.getElementById(
                        "disableConfigButton"
                    ).disabled = false;
                    this.shouldShowError = false;
                }
            }
        },
        toggleCustomizedName: function (checked) {
            if (checked) {
                this.selectedOption = null;
                this.selectedValue = "";
                this.useCustomizedName = true;
            } else {
                this.useCustomizedName = false;
                this.customizedName = "";
            }
            this.selectedOptionValue = null;
        }
    }
});

Vue.component('hostype-help', {
    template: '<help-table v-bind:headers="headers" v-bind:data="data" v-bind:keys="keys" ></help-table>',
    props: ['data'],
    data: function () {
        return {
            headers: [{ name: 'Name', headerClass: 'col-sm-1' },
            { name: 'Id', headerClass: 'col-sm-1' },
            { name: 'Cores', headerClass: 'col-sm-1' },
            { name: 'Memory(GB)', headerClass: 'col-sm-1' },
            { name: 'Storage', headerClass: 'col-sm-2' },
            { name: 'Network', headerClass: 'col-sm-1' },
            { name: 'Blessed Status', headerClass: 'col-sm-1' },
            { name: 'Retired', headerClass: 'col-sm-1' },
            { name: 'Description', headerClass: 'col-sm-5' }],
            keys: ['abstract_name', 'provider_name', 'core', 'mem', 'storage', 'network', 'blessed_status', 'retired', 'description']
        }
    },
})

Vue.component('securityzone-help', {
    template: '<help-table v-bind:headers="headers" v-bind:data="data" v-bind:keys="keys" ></help-table>',
    props: ['data'],
    data: function () {
        return {
            headers: [{ name: 'Name', headerClass: 'col-sm-1' },
            { name: 'Id', headerClass: 'col-sm-1' },
            { name: 'Description', headerClass: 'col-sm-9' }],
            keys: ['abstract_name', 'provider_name', 'description']
        }
    },
})

Vue.component('placements-help', {
    template: '<help-table v-bind:headers="headers" v-bind:data="data" v-bind:keys="keys" ></help-table>',
    props: ['data'],
    data: function () {
        return {
            headers: [{ name: 'Name', headerClass: 'col-sm-2' },
            { name: 'Id', headerClass: 'col-sm-2' },
            { name: 'Capacity', headerClass: 'col-sm-1' },
            { name: 'Assign Public IP', headerClass: 'col-sm-2' },
            { name: 'Description', headerClass: 'col-sm-4' }],
            keys: ['abstract_name', 'provider_name', 'capacity', 'assign_public_ip', 'description',]
        }
    },
});

Vue.component('accessrole-help', {
    template: '<div class="form-group">\
    <div class="col-xs-2">\
    </div>\
    <div class="col-xs-10">\
    {{data}} found <a v-bind:href="accessrolelist">here</a>.\
    </div>\
    </div>',
    props: ['data', 'accessrolelist']
});


/**
* A form control with a label and multiple selection box
*/
Vue.component("placements-select", {
    template: '<div class="form-group">\
    <label class="deployToolTip control-label col-xs-2" data-toggle="tooltip" v-bind:title="title">\
        {{label}}\
    </label>\
    <div class="col-xs-6"><div v-bind:class="groupStyle">\
        <select class="form-control chosen-select"  v-on:change="updateValue($event.target.value)" multiple>\
            <optgroup v-for="(group, name) in selectoptions" :label="name">\
                <option v-for="option in group" v-bind:value="option.value" v-bind:selected="option.isSelected" v-bind:class="option.colorClass">{{option.text}}</option>\
            </optgroup>\
        </select>\
    <span v-if="showhelp" class="input-group-btn">\
    <button class="deployToolTip btn btn-default" type="button" data-toggle="tooltip" v-on:click="helpClick" title="click to see more information">\
        <span class="glyphicon glyphicon-question-sign"></span>\
    </button>\
    </span></div>\
    </div>\
    <div class="col-xs-2" v-if="showsubnettype">\
        <input type="radio" id="public-subnet" value="public" v-model="subnettype" v-on:click="filterclick($event.target.value)">\
        <label for="public">Public subnets</label>\
        <br>\
        <input type="radio" id="private-subnet" value="private" v-model="subnettype" v-on:click="filterclick($event.target.value)">\
        <label for="private">Private subnets</label>\
        <br>\
    </div>\
    <div class="col-xs-2">\
        <input type="checkbox" id="checkbox" v-bind:checked="assignpublicip" v-on:click="assignipchange($event.target.checked)">\
        <label for="checkbox">Assign Public IP</label>\
    </div></div>',
    props: ['label', 'title', 'selectoptions', 'showhelp', 'assignpublicip', 'subnettype', 'showsubnettype'],
    data: function () {
        return {
            groupStyle: this.showhelp ? 'input-group' : ''
        }
    },
    methods: {
        updateValue: function (value) {
            this.$emit('input', value)
        },
        helpClick: function () {
            this.$emit('helpclick')
        },
        assignipchange: function (value) {
            this.$emit('assignpublicipclick', value)
        },
        filterclick: function (value) {
            this.$emit('subnetfilterclick', value)
        }
    }
});

Vue.component("hosttype-select", {
    template: '<div class="form-group">\
    <label class="deployToolTip control-label col-xs-2" data-toggle="tooltip" v-bind:title="title">{{label}}</label>\
    <div class="col-xs-6"><div v-bind:class="groupStyle">\
    <select v-bind:class="selectClass" v-on:change="updateValue($event.target.value)" required="true">\
    <option v-for="option in selectoptions" v-bind:value="option.value" v-bind:selected="option.isSelected" v-bind:disabled="option.isDisabled">{{option.text}}</option></select>\
    <span v-if="showhelp" class="input-group-btn">\
      <button class="deployToolTip btn btn-default" type="button" data-toggle="tooltip" title="click to see more information" v-on:click="helpClick">\
          <span class="glyphicon glyphicon-question-sign"></span>\
      </button>\
    </span></div>\
    </div>\
    <div class="col-xs-1"></div>\
    <div class="col-xs-3" v-if="isVisible">\
        <input type="checkbox" id="checkbox" v-bind:checked="enablemultiplehosttypes" v-on:click="enabletypeschange($event.target.checked)">\
        <label for="checkbox">Enable Backup instance types</label>\
    </div></div>',
    props: ['label', 'title', 'selectoptions', 'showhelp', 'small', 'selectsearch', 'retired', 'enablemultiplehosttypes', 'disablebackupinstancetypes'],
    computed: {
        isVisible: function () {
            console.log(this.disablebackupinstancetypes);
            console.log(this.disablebackupinstancetypes === "False");
            return this.disablebackupinstancetypes === "False"
        }
    },
    data: function () {
        return {
            width: this.small ? 'col-xs-4' : 'col-xs-10',
            formStyle: this.small ? '' : 'form-group',
            groupStyle: this.showhelp ? 'input-group' : '',
            selectClass: this.selectsearch ? 'form-control single-select-search' : 'form-control'
        }
    },
    methods: {
        updateValue: function (value) {
            this.$emit('input', value)
        },
        helpClick: function () {
            this.$emit('helpclick')
        },
        enabletypeschange: function (value) {
            this.$emit('enablemultiplehosttypesclick', value)
        },
    }
});

Vue.component('remaining-capacity', {
    template: '<div class="form-group">\
    <div class="col-xs-2"></div>\
    <div class="col-xs-6" v-bind:style="marginStyle">\
        <span class="col-xs-6" style="padding:0;" v-bind:title="title">Remaining Subnet Capacity: {{remainingcapacity}}</span>\
    </div>\
    </div>',
    props: ['title', 'remainingcapacity', 'inadvanced'],
    computed: {
        marginStyle: function () {
            return this.inadvanced ? 'margin-top:-15px;' : 'margin-top:-30px;'
        }
    }
});

Vue.component('backup-hosttypes', {
    template: '<div class="form-group" v-if="isVisible">\
    <div class="col-xs-2"></div>\
    <div class="col-xs-8">\
        <span class="col-xs-8" style="padding:0;">Backup Host Types In Order: {{backuphosttypes}}</span>\
    </div>\
    </div>',
    props: ['backuphosttypes'],
    computed: {
        isVisible: function () {
            return this.backuphosttypes !== 'None'
        }
    }
});

Vue.component("accessrole-input", {
    template: '<div class="form-group">\
    <label for="access_role" class="deployToolTip control-label col-xs-2"\
    data-toggle="tooltip"\
    v-bind:title="title">\
        {{label}}\
    </label>\
    <div class="col-xs-4">\
        <div v-bind:class="groupStyle">\
        <input class="form-control" id="access_role"  required="false" type="text" \
            v-on:input="updateValue($event.target.value)" v-bind:value="value"/>\
    <span v-if="showhelp" class="input-group-btn">\
        <button id="accessRoleExpressionId" class="deployToolTip btn btn-default"\
                type="button" data-toggle="tooltip" v-on:click="helpClick"\
                title="click to see more information">\
            <span class="glyphicon glyphicon-question-sign"></span>\
        </button>\
    </span>\
    </div>\
    </div>\
    </div>',
    props: ['label', 'value', 'title', 'showhelp'],
    data: function () {
        return {
            groupStyle: this.showhelp ? 'input-group' : ''
        }
    },
    methods: {
        updateValue: function (value) {
            this.$emit('input', value)
        },
        helpClick: function () {
            this.$emit('helpclick')
        },
    }
});

Vue.component("stateful-select", {
    template:
    `<div class="form-group">
        <label-select2 label="Stateful Status" title="stateful status" v-on:input="updateValue(value)" col-class="col-xs-2" show-help="true"
        :options="statefuloptions" :selected="value" @input="$emit('stateful-change', $event)" @help-clicked="helpClick">
        </label-select2>
    </div>`,
    props: ['statefuloptions', 'value'],
    methods: {
        updateValue: function (value) {
            this.$emit('input', value)
        },
        helpClick: function () {
            this.$emit('help-clicked')
        },
    }
});

Vue.component('stateful-help', {
    template:
    `<div class="form-group">
        <div class="col-xs-2">
        </div>
        <div class="col-xs-10">
            STATELESS - your cluster can rotate hosts without data loss or other effects
            <br />
            STATEFUL - your cluster cannot automatically rotate hosts without data loss or other effects (i.e. loss of information in the ram, storage, etc.)
            <br />
        </div>
    </div>`
});
