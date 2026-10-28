function storeToken(token) {
    console.log("Store Token");
    localStorage.setItem('jwtToken', token);

    const jwtTokenInputs = document.getElementsByName("token");
    for (let i = 0; i < jwtTokenInputs.length; i++) {
        jwtTokenInputs[i].value = localStorage.getItem("jwtToken");
    }
}

function clearToken() {
    console.log("Clear Token");
    localStorage.removeItem('jwtToken');
}

window.onload = function() {
    console.log("Initializing...");
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (token) {
        storeToken(token);

        window.history.replaceState({}, document.title, window.location.pathname);
        
        const tokenInput = document.getElementById("tokenInput");
        const tokenInputError = document.getElementById("tokenInputError");

        if (tokenInput) tokenInput.value = token;
        if (tokenInputError) tokenInputError.value = token;
    }
};