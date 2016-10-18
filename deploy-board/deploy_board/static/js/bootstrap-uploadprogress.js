/*
 * bootstrap-uploadprogress
 * github: https://github.com/jakobadam/bootstrap-uploadprogress
 *
 * Copyright (c) 2015 Jakob Aarøe Dam
 * Version 1.0.0
 * Licensed under the MIT license.
 */
(function($){
    "use strict";

    $.support.xhrFileUpload = !!(window.FileReader && window.ProgressEvent);
    $.support.xhrFormData = !!window.FormData;

    if(!$.support.xhrFileUpload || !$.support.xhrFormData){
        // skip decorating form
        return;
    }

    var template = '<div class="modal fade" id="file-progress-modal">\
  <div class="modal-dialog">\
    <div class="modal-content">\
      <div class="modal-header">\
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
        <h4 class="modal-title">Uploading</h4>\
      </div>\
      <div class="modal-body">\
        <div class="modal-message"></div>\
        <div class="progress">\
          <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0"\
               aria-valuemax="100" style="width: 0%;min-width: 2em;">\
            0%\
          </div>\
        </div>\
      </div>\
      <div class="modal-footer" style="display:none">\
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
      </div>\
    </div>\
  </div>\
</div>';

    var Uploadprogress = function(element, options){
        this.options = options;
        this.$element = $(element);
    };

    Uploadprogress.prototype = {

        constructor: function() {
            this.$form = this.$element;
            this.$form.on('submit', $.proxy(this.submit, this));
            this.$modal = $(this.options.template);
            this.$modal_message = this.$modal.find('.modal-message');
            this.$modal_title = this.$modal.find('.modal-title');
            this.$modal_footer = this.$modal.find('.modal-footer');
            this.$modal_bar = this.$modal.find('.progress-bar');

            this.$modal.on('hidden.bs.modal', $.proxy(this.reset, this));
        },

        reset: function(){
            this.$modal_title = this.$modal_title.text('Uploading');
            this.$modal_footer.hide();
            this.$modal_bar.addClass('progress-bar-success');
            this.$modal_bar.removeClass('progress-bar-danger');
            if(this.xhr){
                this.xhr.abort();
            }
        },

        submit: function(e) {
            e.preventDefault();

            this.$modal.modal({
                backdrop: 'static',
                keyboard: false
            });

            // We need the native XMLHttpRequest for the progress event
            var xhr = new XMLHttpRequest();
            this.xhr = xhr;

            xhr.addEventListener('load', $.proxy(this.success, this, xhr));
            xhr.addEventListener('error', $.proxy(this.error, this, xhr));

            xhr.upload.addEventListener('progress', $.proxy(this.progress, this));

            var form = this.$form;
            xhr.open(this.options.method, this.options.action);

            xhr.setRequestHeader('X-REQUESTED-WITH', 'XMLHttpRequest');

            var data = new FormData(form.get(0));
            xhr.send(data);
        },

        success: function(xhr) {
            this.options.onComplete();
            if(xhr.status == 0 || xhr.status >= 400){
                return this.error(xhr);
            }
            this.set_progress(100);
            var url;
            var content_type = xhr.getResponseHeader('Content-Type');

            // make it possible to return the redirect URL in
            // a JSON response
            if(content_type.indexOf('application/json') !== -1){
                var response = $.parseJSON(xhr.responseText);
                url = response.location;
            } else if (this.options.redirect_url) {
                url = this.options.redirect_url;
            } else {
                var responseText = xhr.responseText;
                url = responseText;
            }
            this.$modal.modal('hide');
            window.location.href = url;
        },

        // handle form error
        // we replace the form with the returned one
        error: function(xhr){
            var responseText = xhr.responseText
            this.options.onError(responseText);
            
            this.$modal_title.text('Upload failed');

            this.$modal_bar.removeClass('progress-bar-success');
            this.$modal_bar.addClass('progress-bar-danger');
            this.$modal_footer.show();

            var content_type = xhr.getResponseHeader('Content-Type');
            // Replace the contents of the form, with the returned html
            
            this.$modal.modal('hide');
        },

        set_progress: function(percent){
            this.$modal_bar.attr('aria-valuenow', percent);
            this.$modal_bar.text(percent + '%');
            this.$modal_bar.css('width', percent + '%');
        },

        progress: function(/*ProgressEvent*/e){
            if (Math.round((e.loaded/e.total) * 100) == 100) {
                this.$modal_message.text(this.options.tempStopMessage);
            } else { 
                this.$modal_message.text('Uploading to server...');
            }
            var divider = 100/(this.options.tempStopPercentage);
            var percent = Math.round(((e.loaded / e.total) * 100)/divider);

            this.set_progress(percent);
        },
    };

    $.fn.uploadprogress = function(options, value){
        return this.each(function(){
            var _options = $.extend({}, $.fn.uploadprogress.defaults, options);
            var file_progress = new Uploadprogress(this, _options);
            file_progress.constructor();
        });
    };

    $.fn.uploadprogress.defaults = {
        template: template,
        // if you would like to stop progress bar at a certain percentage because there are other tasks that haven't finished yet
        tempStopPercentage: 100,
        tempStopMessage: '', 
        onComplete: function() {},
        onError: function() {},
        method: 'post',
        action: window.location.href, 
        //redirect_url: ...
        // need to customize stuff? Add here, and change code accordingly.
    };

})(window.jQuery);
