/**
 * Common Components
 *
 */


/**
 * A href link button in the side menu
 */
Vue.component('side-button', {
  template: '<a type="button" class="deployToolTip btn btn-default btn-block"\
                v-bind:href=href\
                data-toggle="tooltip" v-bind:title=title>\
           <i v-bind:class=styleclass></i> {{text}} \
        </a>',
  props: ['styleclass', 'text', 'href', 'title']
});


/**
 * A click button in the side menu
 */
Vue.component('side-button-nolink', {
  template: '<a type="button" class="deployToolTip btn btn-default btn-block"\
                data-toggle="tooltip" v-bind:title=title v-on:click.stop="clickButton">\
           <i v-bind:class=styleclass></i> {{text}} \
        </a>',
  props: ['styleclass', 'text', 'title'],
  methods: {
    'clickButton': function () {
      this.$emit('click')
    }
  }
});

/**
 * A button that clicks will show a confirm modal dialog
 */
Vue.component('modal-panel-heading-button', {
  template: '<button type="button" class="deployToolTip btn btn-default btn-sm" data-toggle="modal"\
   v-bind:data-target="confirmDiaglog" v-bind:title="title" ><span v-bind:class="styleclass"></span> {{text}}\
    </button>',
  props: ['confirmDiaglog', 'title', 'text', 'styleclass']
});

/**
 * A button on the side menu
 */
Vue.component('side-button-modal-confirm', {
  template: '<div class="row">\
    <button class="deployToolTip btn btn-default btn-block" v-bind:data-target="confirmDiaglog"\
        data-toggle="modal" v-bind:title="title">\
        <span v-bind:class="styleclass"></span> {{text}} \
    </button></div>',
  props: ['confirmDiaglog', 'title', 'text', 'styleclass']
}
);

/**
 * A button looks like tag. The tag can be clicked and a remove button attatched
 */
Vue.component('tag-button', {
  template: '<div class="btn-group tag-editor-button" contenteditable="false"><button type="button"\
   class="btn btn-sm tag-hover-button tag-button" v-on:click="onClick($event.target.value)">{{name}}</button>\
      <button type="button" class="btn btn-sm tag-button tag-hover-button" v-on:click="removeTag(index)">\
       <span class="glyphicon glyphicon-remove"></span>\
      </button></div>',
  props: ['name', 'index'],
  methods: {
    removeTag: function (index) {
      this.$parent.tags.splice(index, 1)
    },
    onClick: function (value) {
      this.$emit('tagClick', value)
    }
  }
}
);

/**
 * A tag input box to hold all tag buttons
 */
Vue.component('tag-input-box', {
  inputMessage: '',
  template: '<div class="form-group">\
    <label for="hostCapacity" class="control-label col-xs-2">{{label}}</label> \
    <div class="col-xs-10">\
      <div class="tag-editor-field"> \
      <tag-button v-for="(tag,index) in tags" v-bind:name="tag" v-bind:index="index" v-on:tagClick="tagClick(tag)"></tag-button>\
      <input id="taginput" v-bind:placeholder="placeholder" class="btn btn-sm" v-on:blur="updateTags" \
      v-on:keyup.enter="updateTags" v-on:focus="onFocus" v-model="message"></input></div>\
    </div>\
  </div>',
  props: ['label', 'inittags', 'placeholder'],
  data: function () {
    return {
      message: "",
      tags: this.inittags,
      noFocus: true,
    }
  },
  methods: {
    onFocus: function (e) {
      this.noFocus = false
    },
    updateTags: function (e) {
      this.noFocus = true
      var values = this.message.split(",");
      var length = values.length;
      var validValues = [];
      for (var i = 0; i < length; i++) {
        var trimmed = values[i].trim()
        if (trimmed.length > 0) {
          validValues.push(trimmed);
        }
      }
      this.tags = this.tags.concat(validValues);
      this.message = "";
    },
    tagClick: function (value) {
      this.$emit('inputtagclick', value)
    }
  }
});

/**
 * A form control that has a label text and an input box
 */
Vue.component('label-input', {
  template: '<div class="form-group"><label class="control-label col-xs-2">{{label}}</label>\
  <div class="col-xs-4">\
   <input class="form-control" required="false" v-bind:placeholder="placeholder" type="text" \
    v-on:input="updateValue($event.target.value)" v-bind:value="value"></input>\
  </div></div>',
  props: ['label', 'value', 'placeholder'],
  methods: {
    updateValue: function (value) {
      this.$emit('input', value)
    }
  }
});

/**
 * A form control that has a label and text. This is for read only settings
 */
Vue.component('label-info', {
  template: '<div class="form-group">\
      <label class="deployToolTip text-right col-xs-4" v-bind:title="title">\
      {{name}}</label>\
      <span class="deployToolTip col-xs-2" v-bind:class="styleclass"><strong>{{text}}</strong></span>\
      </div>',
  props: ['title', 'name', 'text', 'styleclass']
});

/**
 * A form control with a label and a select box
 */
Vue.component('label-select', {
  template: '<div v-bind:class="formStyle">\
  <label class="deployToolTip control-label col-xs-2" data-toggle="tooltip" v-bind:title="title">{{label}}</label>\
  <div v-bind:class="width"><div v-bind:class="groupStyle">\
  <select :disabled="disabled" class="form-control" v-on:change="updateValue($event.target.value)" required="true">\
  <option v-for="option in selectoptions" v-bind:value="option.value" v-bind:selected="option.isSelected">{{option.text}}</option></select>\
  <span v-if="showhelp" class="input-group-btn">\
    <button class="deployToolTip btn btn-default" type="button" data-toggle="tooltip" title="click to see more information" v-on:click="helpClick">\
        <span class="glyphicon glyphicon-question-sign"></span>\
    </button>\
  </span></div>\
  </div></div>',
  props: ['label', 'title', 'selectoptions', 'showhelp', 'small', 'disabled'],
  data: function () {
    return {
      width: this.small ? 'col-xs-4' : 'col-xs-10',
      formStyle: this.small ? '' : 'form-group',
      groupStyle: this.showhelp ? 'input-group' : ''
    }
  },
  methods: {
    updateValue: function (value) {
      this.$emit('input', value)
    },
    helpClick: function(){
      this.$emit('helpclick')
    }
  }
});


/**
 * An improved version for label-select
 */
Vue.component('label-select2', {
  template:
  `<div>
    <label class="deployToolTip control-label col-xs-2" data-toggle="tooltip" :title="title">{{label}}</label>
    <div :class="colClass">
      <div :class="groupStyle">
        <select class="form-control" required="true"
            :value="selected" @change="$emit('input', $event.target.value)"
            :disabled="disabled">
          <option v-for="option in options" :value="option.value" :selected="option.value == selected">{{option.text}}</option>
        </select>
        <span v-if="showHelp" class="input-group-btn">
          <button class="deployToolTip btn btn-default" type="button" data-toggle="tooltip" title="click to see more information" @click="helpClick">
            <span class="glyphicon glyphicon-question-sign"></span>
          </button>
        </span>
      </div>
    </div>
  </div>`,
  props: ['label', 'title', 'showHelp', 'colClass', 'disabled', 'options', 'selected'],
  data: function () {
    return {
      groupStyle: this.showHelp ? 'input-group' : ''
    }
  },
  methods: {
    helpClick: function () {
      this.$emit('help-clicked')
    }
  }
});


/**
 * A basic checkbox that allows to bind with boolean v-model
 */
Vue.component('base-checkbox', {
    template: `
    <input type="checkbox"
      :checked="checked" :disabled="!enabled"
      @change="$emit('input', $event.target.checked)"
    >`,
    model: {
      prop: 'checked',
      event: 'change'
    },
    props: {
      checked: Boolean,
      enabled: Boolean
    }
  })


/**
 * A modal dialog box. The input event contains a the parameter where true means the user confirms
 */
Vue.component('modal', {
  template: '<div v-bind:id="id" class="modal fade">\
    <div class="modal-dialog">\
        <div class="modal-content">\
                <div class="modal-header">\
                    <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span>\
                    <span class="sr-only">Close</span></button>\
                    <h4 class="modal-title">{{title}}</h4>\
                </div>\
                <div class="modal-body"> <slot name="body">default body</slot>\
                </div>\
                <div class="modal-footer">\
                    <button type="button" class="btn btn-md btn-primary" data-dismiss="modal" v-on:click="yes()">&nbsp&nbspOK&nbsp&nbsp</button>\
                    <button type="button" class="btn btn-md btn-default" data-dismiss="modal" v-on:click="no()">Cancel</button>\
                </div>\
        </div>\
  </div></div>',
  props: ['id', 'title'],
  methods: {
    yes: function () {
      this.$emit('input', true);
    },
    no: function () {
      this.$emit('input', false);
    }
  }
});

/**
 * The collapseable panel heading
 */
Vue.component("panel-heading", {
  template: '<div class="panel-heading clearfix">\
    <h4 class="panel-title pull-left pointer-cursor">\
        <a v-on:click="toggle" data-toggle="collapse" v-bind:data-target="target">\
            <span v-bind:class="getclass(collapse)">\
            </span>\
            {{ title }}\
        </a>\
    </h4>\
    <div v-if="checkAlarm", id="alarmsId"></div>\
</div>',
  props: ['title', 'target', 'initcollapse'],
  data: function () {
    return {
      checkAlarm: false,
      collapse: this.initcollapse,
    }
  },
  methods: {
    getclass: function (collapse) {
      if (collapse)
        return "glyphicon glyphicon-chevron-down"
      else
        return "glyphicon glyphicon-chevron-right"
    },
    toggle: function () {
      this.collapse = !this.collapse
    }
  }
});

/**
 * A form table
 */
Vue.component("help-table", {
  template:'<div class="form-group"><div class="col-xs-2"></div><div class="col-xs-10"><table class="table table-condensed table-striped table-hover">\
    <tbody><tr><th v-for="header in headers" v-bind:class="header.headerClass"><p><i>{{header.name}}</i></p></th></tr>\
    <tr v-for="row in data">\
      <td v-for="key in keys"><p><i>{{ row[key] }}</i></p></td>\
    </tr></tbody></table></div></div>',
  props: ['headers', 'data', 'keys']
})

/**
 * A form inline warning alert
 */
 Vue.component("form-warning", {
    template:'<div class="form-group"><div class="col-xs-2"></div>\
    <div class="col-xs-10">\
        <div class="alert alert-warning" style="white-space: pre-wrap;">\
            <button v-if="dismissible" type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
            <strong>Warning!</strong> {{ alertText }}</div>\
    </div></div>',
    props: ['dismissible', 'alertText']
  })

/**
 * A form inline error alert
 */
 Vue.component("form-danger", {
    template:'<div class="form-group"><div class="col-xs-2"></div>\
    <div class="col-xs-10">\
        <div class="alert alert-danger" style="white-space: pre-wrap;">\
            <button v-if="dismissible" type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
            <strong>Danger!</strong> {{ alertText }}</div>\
    </div></div>',
    props: ['dismissible', 'alertText']
  })


/**
 * pinfo warning banner
 */
Vue.component('deployservice-warning-banner', {
    template: `<div v-if="hasPotentialConflicts" id="deployServiceBanner" class="alert alert-warning" role="alert" align="center">
    This cluster has userdata which defines a 'deploy_service' value and a pinfo_role that isn't 'cmp_base'. 
    This could cause conflicts. Please read <a target="_blank" :href="deployservicewikiurl">this wiki</a> for more info.
    </div>`,
    props: ['alluserdata', 'deployservicewikiurl'],
    computed: {
        hasPotentialConflicts: function() {
            const externalFacts = this.alluserdata?.find(field => field.name == 'external_facts')?.value ?? "";
            containsDeployService = /['"]deploy_service['"]:/.test(externalFacts);
            const pinfoRole = this.alluserdata?.find(field => field.name === 'pinfo_role');
            return pinfoRole?.value !== 'cmp_base' && containsDeployService;
        },
    }
});
