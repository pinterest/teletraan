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

Vue.component('cell-select', {
    template: '<div>\
  <label-select label="Cell" title="Cell" \
  v-bind:value="value" v-bind:selectoptions="cells" v-on:input="updateCellValue" v-on:input="updateValue(value)"></label-select></div>',
    props: ['cells', 'value'],
    methods: {
        updateValue: function (value) {
            this.$emit('input', value);
        },
        updateCellValue: function(value) {
            this.$emit('cellchange', value);
            this.$emit('imagenamechange', value)
        }
    }
});



Vue.component('baseimage-select', {
    template: '<div class="form-group">\
<label-select small="true" showhelp="true" label="Image Name" title="Image Name" v-bind:value="imagenamevalue" v-bind:selectoptions="imagenames"  \
 v-bind:selected="imagenamevalue" v-on:input="updateImageName" v-show="inadvanced" v-on:helpclick="helpClick"> </label-select>\
<label-select small="true"  showhelp="true" label="Image" title="Base Image" v-bind:value="baseimagevalue"\
    v-bind:selectoptions="baseimages" v-on:input="updateBaseImage" v-bind:selected="baseimagevalue" v-on:helpclick="helpClick"></label-select>\
</div>',
    props: ['imagenames', 'baseimages', 'imagenamevalue', 'baseimagevalue', 'inadvanced'],
    methods: {
        updateBaseImage: function (value) {
            this.$emit('baseimagechange', value)
        },
        updateImageName: function (value) {
            this.$emit('imagenamechange', value)
        },
        helpClick: function (value) {
            this.$emit('helpclick')
        }
    }
});

Vue.component('base-image-help', {
    template: '<help-table v-bind:headers="baseImageHelpHeaders" v-bind:data="data" v-bind:keys="keys" ></help-table>',
    props: ['data'],
    data: function () {
        return {
            baseImageHelpHeaders: [{ name: 'Publish Date', headerClass: 'col-sm-2' },
            { name: 'Name', headerClass: 'col-sm-1' },
            { name: 'Id', headerClass: 'col-sm-1' },
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
            <input class="form-control" type="text" v-bind:value="value" v-on:input="change(name,$event.target.value)" v-bind:readonly="readonly ? true : false"></input>\
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
  </div>',
    props: ['alluserdata', 'inadvanced', 'showcmpgroup'],
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
        }
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
                                v-model="shouldShowError" v-model="errorMessage" v-show="shouldShowError">{{errorMessage}}</label></div>\
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
            </form></div></div>',
    props: ["options", "id"],
    data: function() {
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
        updateValue: function(value) {
            this.selectedValue = value;
            if (value === "") {
                this.selectedOptionValue = "";
            } else {
                this.selectedOption = this.options.filter(function(item) {
                    return item.name === value;
                })[0];
                this.selectedOptionValue = this.selectedOption.default;
            }
        },
        addConfig: function() {
            if (this.useCustomizedName) {
                if (
                    this.customizedName != "spiffe_id" &&
                    this.customizedName != "nimbus_id" &&
                    this.customizedName != "assign_public_ip"
                ) {
                    this.$emit("click", {
                        name: this.customizedName,
                        value: this.selectedOptionValue
                    });
                } else {
                    document.getElementById(
                        "disableConfigButton"
                    ).disabled = true;
                }
            } else {
                this.$emit("click", {
                    name: this.selectedOption.name,
                    value: this.selectedOptionValue
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
        toggleCustomizedName: function(checked) {
            if (checked) {
                this.selectedOption = null;
                this.selectedValue = "";
                this.useCustomizedName = true;
            } else {
                this.useCustomizedName = true;
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
            { name: 'Description', headerClass: 'col-sm-5' }],
            keys: ['abstract_name', 'provider_name', 'core', 'mem', 'storage', 'description']
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


/**
* A form control with a label and multiple selection box
*/
Vue.component("placements-select", {
    template: '<div class="form-group">\
    <label class="deployToolTip control-label col-xs-2" data-toggle="tooltip" v-bind:title="title">\
      {{label}}\
    </label>\
    <div class="col-xs-7"><div v-bind:class="groupStyle">\
          <select class="form-control chosen-select"  v-on:change="updateValue($event.target.value)" multiple>\
            <option v-for="option in selectoptions" v-bind:value="option.value" v-bind:selected="option.isSelected">{{option.text}}</option>\
          </select>\
    <span v-if="showhelp" class="input-group-btn">\
    <button class="deployToolTip btn btn-default" type="button" data-toggle="tooltip" v-on:click="helpClick" title="click to see more information">\
        <span class="glyphicon glyphicon-question-sign"></span>\
    </button>\
   </span></div>\
    </div>\
    <div class="col-xs-3">\
      <input type="checkbox" id="checkbox" v-bind:checked="assignpublicip" v-on:click="assignipchange($event.target.checked)">\
      <label for="checkbox">Assign Public IP</label>\
    <div></div>',
    props: ['label', 'title', 'selectoptions', 'showhelp', 'assignpublicip'],
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
        }
    }
});


