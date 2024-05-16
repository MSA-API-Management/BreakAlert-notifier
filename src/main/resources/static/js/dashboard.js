function showTab(tabName) {
    document.getElementById("dashboardTab").style.display = tabName === "dashboard" ? "block" : "none";
    document.getElementById("subscribeTab").style.display = tabName === "subscribe" ? "block" : "none";
    document.getElementById("clientTab").style.display = tabName === "client" ? "block" : "none";
}

function storeToken(token) {
    console.log("stored token");
    localStorage.setItem('jwtToken', token);

    var jwtTokenInputs = document.getElementsByName("jwtToken");
    for (var i = 0; i < jwtTokenInputs.length; i++) {
        jwtTokenInputs[i].value = localStorage.getItem("jwtToken");
    }
}

function clearToken() {
    console.log("Clear Token");
    localStorage.removeItem('jwtToken');
}

window.onload = function() {
    console.log("on start up");
    const urlParams = new URLSearchParams(window.location.search);
    token = urlParams.get('token');
    console.log("Token: " + token);
    if (token) {
        storeToken(token);
        // Remove the token parameter from the URL
        window.history.replaceState({}, document.title, "/dashboard");
        document.getElementById("tokenInput").value = token;
    }
};