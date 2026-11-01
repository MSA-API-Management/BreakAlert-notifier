function showTab(tabName) {
    document.getElementById("dashboardTab").style.display = tabName === "dashboard" ? "block" : "none";
    document.getElementById("subscribeTab").style.display = tabName === "subscribe" ? "block" : "none";
    document.getElementById("clientTab").style.display = tabName === "client" ? "block" : "none";
}

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.subscription-card').forEach(function(card) {
        card.addEventListener('click', function(event) {
            if (event.target.closest('.delete-btn')) {
                return;
            }

            const subscriptionItem = card.closest('li');
            const dataChanges = subscriptionItem.querySelector('.data-changes');
            const chevron = card.querySelector('.chevron-icon');

            if (dataChanges) {
                dataChanges.classList.toggle('hidden');

                const isCurrentlyHidden = dataChanges.classList.contains('hidden');
                dataChanges.style.display = isCurrentlyHidden ? 'none' : 'block';
            }

            if (chevron) {
                chevron.classList.toggle('rotate-90');
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

        const tokenInput = document.getElementById("tokenInput");
        if (tokenInput) {
            tokenInput.value = token;
        }
    }
};