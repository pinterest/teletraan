/**
 * Helper scripts for client-side validation of input data.
 */

function validateInput(input, validator, success, failure) {
    if (validator(input)) {
        success();
    } else {
        failure();
    }
}

function validName(name) {
    // Valid name if it contains letters (A-Za-z0-9), underscores (_), and hyphens (-) only
    if (name.search(/^[A-Za-z0-9_\-]+$/)) {
        return false;
    }
    return true;
}

function validStageType(stageType) {
    // Valid if it is not DEFAULT
    if (stageType == "DEFAULT") {
        return false;
    }
    return true;
}