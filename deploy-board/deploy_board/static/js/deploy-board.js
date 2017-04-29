$(document).ready(function () {
    $('.deployToolTip').tooltip({ container: "#toolTipContent", delay: { show: 400, hide: 10 } });
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

function getCookie(name) {
    var cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        var cookies = document.cookie.split(';');
        for (var i = 0; i < cookies.length; i++) {
            var cookie = jQuery.trim(cookies[i]);
            // Does this cookie string begin with the name we want?
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

$(document).ajaxError(function (event, jqxhr, settings, thrownError) {
    //ignore the metrics update faiure for now
    if (settings.url.indexOf('get_service_metrics') > -1) {
        console.log(jqxhr.responseText);
        return;
    }
});

function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
}

function getDefaultPlacement(capacityCreationInfo) {
    this.capacityCreationInfo = capacityCreationInfo
    var cmpPublicIPPlacements = {}
    var cmpPrivateIPPlacements = {}
    var allPrivateIPPlacements = []
    var allPublicIPPlacements = []

    //Save the maximum subnet for each abstract_name
    function addToMaxGroup(item, group) {
        if (item.abstract_name in group) {
            if (item.capacity > group[item.abstract_name].capacity)
                group[item.abstract_name] = item
        }
        else {
            group[item.abstract_name] = item
        }
    }

    function getTopSelection(group) {
        var items = []
        $.each(group, function (abstract_name, value) {
            items.push(value)
        })
        return items.sort(function (item1, item2) { return item1.capacity < item2.capacity; })
    }


    //This function creates the default placements. The default algorithm is as below:
    //1. Find the placement has the highest capacity under each abstract_name
    //2. For each group of placements, pick the top 3
    //For ex below {provider_name,capacity} represents a placement from provider:
    //   us_east_1a = [{subnet1:100}, {subnet2:60}]
    //   us_east_1c = [{subnet3:60}, {subnet4:2}]
    //   us_east_1d = [{subnet5:90}, {subnet6:20}]
    //   us_east_1e = [{subnet7:120}, {subnet8:30}]
    //   It will pick subnet1, subnet5 and subnet7
    //   Also besides availablity zone, we grouped by public ip or not
    $.each(this.capacityCreationInfo.placements, function (index, item) {
        if (item.assign_public_ip) {
            allPublicIPPlacements.push(item)
            addToMaxGroup(item, cmpPublicIPPlacements)
        }
        else {
            allPrivateIPPlacements.push(item)
            addToMaxGroup(item, cmpPrivateIPPlacements)
        }
    })
    return {
        cmpPrivate: getTopSelection(cmpPrivateIPPlacements),
        cmpPublic: getTopSelection(cmpPublicIPPlacements),
        allPrivate: allPrivateIPPlacements,
        allPublic: allPublicIPPlacements,
        inDefault: function (item) {
            var foundIdx = -1
            if (item.assign_public_ip) {
                this.cmpPublic.find(function (value, idx) {
                    if (item.id === value.id) {
                        foundIdx = idx;
                        return true
                    }
                    return false
                })
            }
            else {
                this.cmpPrivate.find(function (value, idx) {
                    if (item.id === value.id) {
                        foundIdx = idx;
                        return true
                    }
                    return false
                })
            }
            return foundIdx >= 0 && foundIdx < 3
        },
        getSimpleList: function (assignPublicIp, existingItems) {
            //Return a simple list fo selection.
            //Simple list is grouped by the abstract_name, 
            //for each abstract_name, we have one candidate.
            //If existingItems is null or empty, the abstract_name
            //will be the existing one
            var arr = assignPublicIp ? this.cmpPublic : this.cmpPrivate
            var fullArr = assignPublicIp ? this.allPublic : this.allPrivate
            if (existingItems != null && existingItems.length > 0) {

                existingItems = existingItems.map(function (item) {
                    var fullInfo = fullArr.find(
                        function (i) {
                            return i.id === item
                        }
                    )

                    return fullInfo != null ? fullInfo : { id: item }
                })

                //Only have id. Append abstract_name to existingItems
                arr = arr.map(function (item) {
                    var existing = existingItems.find(
                        function (value, index) {
                            return item.abstract_name === value.abstract_name
                        })
                    if (existing != null) {
                        return {
                            value: existing.id,
                            text: existing.abstract_name,
                            isSelected: true
                        }
                    }
                    else {
                        return {
                            value: item.id,
                            text: item.abstract_name,
                            isSelected: false
                        };
                    }
                })
            }
            else {
                arr = arr.map(function (item, idx) {
                    return {
                        value: item.id,
                        text: item.abstract_name,
                        isSelected: this.isDefault(item)
                    }
                })
            }
            return arr;

        },
        getFullList: function (assignPublicIp, existingItems) {
            var arr = assignPublicIp ? this.allPublic : this.allPrivate
            if (existingItems != null && existingItems.length > 0) {
                existingItems = existingItems.map(function (item) {
                    var fullInfo = arr.find(
                        function (i) {
                            return i.id === item
                        }
                    )

                    return fullInfo != null ? fullInfo : { id: item }
                })
                arr = arr.map(function (item) {
                    var existing = existingItems.find(
                        function (value) {
                            return item.id === value.id
                        })
                    if (existing != null) {
                        return {
                            value: existing.id,
                            text: existing.provider_name,
                            isSelected: true
                        }
                    }
                    else {
                        return {
                            value: item.id,
                            text: item.provider_name,
                            isSelected: false
                        };
                    }
                })
            }
            else {
                arr = arr.map(function (item, idx) {
                    return {
                        value: item.id,
                        text: item.provider_name,
                        isSelected: this.isDefault(item)
                    }
                })
            }
            return arr;
        }
    }
}

