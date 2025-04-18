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
function setUrlParameter(url, name, val){
    let searchString = url.search.substring(1);
    let vars = searchString.split("&")
    let modifiedString = "?"
    let set= false;

    for(let i=0; i < vars.length; i++){
        if(i>0){
            modifiedString+="&"
        }
        let currParam = vars[i].split('=');
        if(currParam[0] === name){
            modifiedString+=name
            modifiedString+= "=" + encodeURIComponent(val)
            set=true;
        } else {
            modifiedString+=vars[i]
        }
    }
    if(!set){
        if(modifiedString.length>1){
            modifiedString+="&"
        }
        modifiedString+=name
        modifiedString+="="+encodeURIComponent(val)
    }
    url.search=modifiedString;
    return url
}

function deleteUrlParameter(url, name){
    let searchString = url.search.substring(1);
    let vars = searchString.split("&")
    let modifiedString = "?"

    for(let i=0; i < vars.length; i++){
        let currParam = vars[i].split('=');
        if(currParam[0]!==name){
            if(modifiedString.length>1){
                modifiedString+="&"
            }
            modifiedString+=vars[i]
        }
    }
    url.search=modifiedString;
    return url
}




function getRemainingCapacity(capacityInfo, placementList) {
    if (!capacityInfo || !placementList) {
        return Infinity;
    }
    let filteredPlacements = capacityInfo.filter(placement => placementList.includes(placement.id));
    let totalCapacity = filteredPlacements.reduce((s, e) => s + e.capacity, 0);
    return totalCapacity;
}

function getBackupIds(mapping, types, type, enable) {
    const selectedHostTypeObject = types.find(hostType => hostType.id === type);
    const name = selectedHostTypeObject.provider_name;
    const selectedHostTypeMappingObject = mapping.find(hostTypeMapping => hostTypeMapping.defaultId === name);
    if (enable && selectedHostTypeMappingObject === undefined) {
        return "No back up host types are defined for this default host type";
    } else if (enable && !(selectedHostTypeMappingObject === undefined)) {
        return selectedHostTypeMappingObject.backupIds;
    } else {
        return "None";
    }
}

function checkHostType(types, type) {
    const selectedHostTypeObject = types.find(hostType => hostType.id === type);
    const name = selectedHostTypeObject.provider_name;
    const prefixes = ["c8g", "m8g", "r8g", "x8g"];
    return prefixes.some(prefix => name.startsWith(prefix));
}

function getDefaultPlacement(capacityCreationInfo) {
    this.capacityCreationInfo = {
        ...this.capacityCreationInfo,
        ...capacityCreationInfo
    };
    var cmpPublicIPPlacements = {}
    var cmpPrivateIPPlacements = {}
    var topCmpPublicIPPlacements = []
    var topCmpPrivateIPPlacements = []
    var allPrivateIPPlacements = []
    var allPublicIPPlacements = []

    //The abstract name of group can be default assigned
    var defaultAssignedGroupForNonC5Instance = new Set(['us-east-1a', 'us-east-1c', 'us-east-1d',
        'us-east-1e', 'us-east-2a', 'us-east-2b', 'us-west-2b', 'us-west-2a']);

    var defaultAssignedGroupForC5Instance = new Set(['us-east-1a', 'us-east-1d',
        'us-east-1e', 'us-east-2a', 'us-east-2b', 'us-west-2b', 'us-west-2a']);

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

    function determineColorClass(capacity) {
        if (capacity < 50) {
            return 'text-danger';
        }
        else if (capacity < 200) {
            return 'text-warning';
        }
        return 'text-primary';
    }

    function getAZ(abstractName, azRegex) {
        var matched = abstractName.match(azRegex);
        return matched ? matched[0] : 'unknown';
    }

    function convertToPlacementOptionsAdv(placements) {
        var options = {}
        if (placements === null || placements.length < 1 ) {
            return options;
        }
        const cellName = placements ? placements[0].cell_name : '1';
        const cellNum = cellName[cellName.length - 1];
        const azRegex = new RegExp(`\\b(${cellNum}[a-g])\\b`);

        for (const placement of placements) {
            var obj = {
                value: placement.id,
                text: `${placement.provider_name} | cap.: ${placement.capacity} | ${placement.abstract_name}`,
                isSelected: placement.isSelected,
                colorClass: determineColorClass(placement.capacity)
            };
            const group = `${getAZ(placement.abstract_name, azRegex)} (Parsed info, for reference only)`;
            if ( !(group in options)) {
                options[group] = [];
            }
            options[group].push(obj);
        }
        return options;
    }

    function convertToPlacementOptions(placements) {
        return {'Basic Settings': placements};
    }

    function inDefault(item) {
        const searchSource = item.assign_public_ip ? topCmpPublicIPPlacements : topCmpPrivateIPPlacements;
        var foundIdx = searchSource.findIndex(function (value) {
            return item.id === value.id;
        });
        return foundIdx >= 0 && foundIdx < 3;
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
            // TODO this should not be hardcoded around the codebase
            if (capacityCreationInfo.defaultHostType === "EbsComputeLo(Recommended)" ) {
                if (defaultAssignedGroupForC5Instance.has(item.abstract_name)) {
                    addToMaxGroup(item, cmpPublicIPPlacements)
                }
            }else if (defaultAssignedGroupForNonC5Instance.has(item.abstract_name)) {
                addToMaxGroup(item, cmpPublicIPPlacements)
            }
        }
        else {
            allPrivateIPPlacements.push(item)
            // TODO this should not be hardcoded around the codebase
            if (capacityCreationInfo.defaultHostType === "EbsComputeLo(Recommended)" ) {
                if (defaultAssignedGroupForC5Instance.has(item.abstract_name)) {
                    addToMaxGroup(item, cmpPrivateIPPlacements)
                }
            }else if (defaultAssignedGroupForNonC5Instance.has(item.abstract_name)) {
                addToMaxGroup(item, cmpPrivateIPPlacements)
            }
        }
    })

    topCmpPublicIPPlacements = getTopSelection(cmpPublicIPPlacements)
    topCmpPrivateIPPlacements = getTopSelection(cmpPrivateIPPlacements)

    return {
        getSimpleList: function (showPublicOnly, existingItems) {
            //Return a simple list fo selection.
            //Simple list is grouped by the abstract_name,
            //for each abstract_name, we have one candidate.
            //If existingItems is null or empty, the abstract_name
            //will be the existing one
            if (typeof showPublicOnly !== "boolean") {
                console.error("getSimpleList expects parameter showPublicOnly to be of boolean type.")
            }
            var arr = showPublicOnly ? topCmpPublicIPPlacements : topCmpPrivateIPPlacements;
            var fullArr = showPublicOnly ? allPublicIPPlacements : allPrivateIPPlacements;
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
                        isSelected: inDefault(item)
                    }
                })
            }
            return convertToPlacementOptions(arr);
        },
        getFullList: function (showPublicOnly, existingItems) {
            if (typeof showPublicOnly !== "boolean") {
                console.error("getFullList expects parameter showPublicOnly to be of boolean type.");
            }
            var placements = showPublicOnly ? allPublicIPPlacements : allPrivateIPPlacements;
            if (existingItems != null && existingItems.length > 0) {
                const existingItemsSet = new Set(existingItems);
                placements = placements.map(function (item) {
                    item['isSelected'] = existingItemsSet.has(item.id) ? true : false;
                    return item;
                });
            }
            else {
                placements = placements.map(function (item) {
                    item['isSelected'] = inDefault(item);
                    return item;
                });
            }
            return convertToPlacementOptionsAdv(placements);
        }
    }
}

function getAccount(accountId) {
    return info.accounts != null ?
        info.accounts.find(function (o) { return o.id === accountId })
        : null;
}

function getAccountOwnerId(accountId) {
    const account = getAccount(accountId);
    return account ? account.ownerId : null;
}

function getDefaultHostType(hostTypes, defaultHostType, defaultARMHostType) {
    this.hostTypes = hostTypes;
    this.defaultHostType = defaultHostType;
    this.defaultARMHostType = defaultARMHostType;
        var isProcessed = false;
        var selected = false;
        var lowestHostType = false;
        this.checkIsDisabledHostType = function(item){
            return item.retired || item.blessed_status === "DECOMMISSIONING";
        };

        var isSelected = function(item) {
            return (
                ("x86_64" === item.arch_name && item.abstract_name === this.defaultHostType)
                || ("arm64" === item.arch_name && item.abstract_name === this.defaultARMHostType)
                ) && !checkIsDisabledHostType(item);
        }

        this.options = this.hostTypes.map(function(item,idx){
            if(isSelected(item)){
                selected = item;
            } else if(!checkIsDisabledHostType(item)){
                if(lowestHostType === false){
                    lowestHostType = item;
                } else if (lowestHostType.core < item.core){
                    lowestHostType = item;
                }
            }
            isProcessed = true;
            return {
            value: item.id,
            text: item.abstract_name+" ("+item.core+" cores, "+item.mem+" GB, "+item.network+", " +item.storage+")",
            isSelected: isSelected(item),
            isDisabled: checkIsDisabledHostType(item)
        };
    });

    return {
        getOptions: function () {
            return options;
        },
        getSelected: function () {
            if(isProcessed === false){
                this.getOptions();
                isProcessed = true;
            }
            if(selected !== false){
                return selected;
            }
            return lowestHostType;
        },
        isDisabledHostType: function(item){
            return checkIsDisabledHostType(item);
        },
        getSelectedId: function() {
            var selected = this.getSelected();
            return selected != undefined ? selected.id : "";
        }
    }
}
