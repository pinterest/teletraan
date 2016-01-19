$(document).ready(function () {
    $('.deployToolTip').tooltip({container: "#toolTipContent", delay: { show: 400, hide: 10 }});
});

var entityMap = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': '&quot;',
    "'": '&#39;',
    "/": '&#x2F;'
};

function escapeHtml(string) {
    return String(string).replace(/[&<>"'\/]/g, function (s) {
        return entityMap[s];
    });
}

$(document).ajaxError(function (event, jqxhr, settings, thrownError) {
    //ignore the metrics update faiure for now
    if (settings.url.indexOf('get_service_metrics') > -1) {
        console.log(jqxhr.responseText);
        return;
    }
});
