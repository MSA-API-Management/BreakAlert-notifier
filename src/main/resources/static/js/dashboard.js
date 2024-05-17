function showTab(tabName) {
    document.getElementById("dashboardTab").style.display = tabName === "dashboard" ? "block" : "none";
    document.getElementById("subscribeTab").style.display = tabName === "subscribe" ? "block" : "none";
    document.getElementById("clientTab").style.display = tabName === "client" ? "block" : "none";
}

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.toggle-button').forEach(function(button) {
        button.addEventListener('click', function(event) {
            event.stopPropagation();
            event.preventDefault();
            // Get current subscription
            const subscriptionItem = button.closest('.subscription-item');
            // Extract the corresponding data change class
            const dataChanges = subscriptionItem.querySelector('.data-changes');
            if (dataChanges) {
                const isVisible = dataChanges.style.display === 'block';
                dataChanges.style.display = isVisible ? 'none' : 'block';
                button.textContent = isVisible ? 'Show Data Changes' : 'Hide Data Changes';
            }
        });
    });
});

function storeToken(token) {
    console.log("Store Token");
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
    console.log("Initializing...");
    const urlParams = new URLSearchParams(window.location.search);
    token = urlParams.get('token');
    if (token) {
        storeToken(token);
        // Remove the token parameter from the URL
        window.history.replaceState({}, document.title, "/dashboard");
        document.getElementById("tokenInput").value = token;
    }
};