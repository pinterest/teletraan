function clearSecurityKeys(ev) {
    const docEl = document.getElementById("apidoc");
    docEl.removeAllSecurityKeys();
}

function pickSpec(event) {
    const specUrl = event.target
        .closest("tr")
        .querySelector("#spec-url-override").value;
    if (specUrl) {
        loadSpecManually(specUrl);
    }
}

function loadSpecManually(specUrl) {
    const docEl = document.getElementById("apidoc");
    let headers = new Headers();
    headers.append("Authorization", docEl.getAttribute("api-key-value"));
    fetch(specUrl, { headers: headers })
        .then((response) => response.json())
        .then((specJson) => {
            const specUrlObj = new URL(specUrl);
            specJson.servers = [{ url: specUrlObj.origin }];
            docEl.loadSpec(specJson);
        });
}

function setSecurity(ev, securityType) {
    const docEl = document.getElementById("apidoc");
    const trEl = ev.target.closest("tr");
    if (securityType === "apikey") {
        const securitySchemeId = trEl.querySelector(
            "#api-security-scheme-id"
        ).value;
        const newApiToken = trEl.querySelector("#api-key-value").value;
        docEl.setAttribute("api-key-name", "Authorization");
        docEl.setAttribute("api-key-location", "header");
        if (securitySchemeId === "http-bearer") {
            docEl.setAttribute("api-key-value", "Bearer " + newApiToken);
        } else {
            docEl.setAttribute("api-key-value", "token " + newApiToken);
        }
    }
    ev.target.innerText = "Applied !";
    ev.target.style.backgroundColor = "green";
    window.setTimeout(function () {
        ev.target.innerText = "Apply";
        ev.target.style.backgroundColor = "#2069e0";
    }, 5000);
}

window.addEventListener("load", function () {
    const specUrl =
        document.getElementById("spec-url-override").options[0].value;
    if (specUrl) {
        loadSpecManually(specUrl);
    }
});
