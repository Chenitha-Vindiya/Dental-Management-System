// 1. Check state on load
document.addEventListener("DOMContentLoaded", function() {
    const sidebar = document.getElementById('mainSidebar');

    // Explicitly check for the string "true"
    if (localStorage.getItem("sidebar-collapsed") === "true") {
        sidebar.classList.add('collapsed');
    } else {
        sidebar.classList.remove('collapsed');
    }
});

// 2. Attach function to the global window object to ensure it always fires
window.toggleSidebar = function() {
    const sidebar = document.getElementById('mainSidebar');

    // Toggle the class on the sidebar
    sidebar.classList.toggle('collapsed');

    // Explicitly save the exact text string to prevent boolean glitches
    if (sidebar.classList.contains('collapsed')) {
        localStorage.setItem("sidebar-collapsed", "true");
    } else {
        localStorage.setItem("sidebar-collapsed", "false");
    }
};