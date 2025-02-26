document.addEventListener("DOMContentLoaded", () => {
    // Create context menu container
    const contextMenu = document.createElement("div");
    contextMenu.classList.add("context-menu");
    contextMenu.style.display = "none";

    // Menu items
    const menuItems = [
        { text: "Home", link: "/" },
        { text: "Chat", link: "/chat" },
        { text: "People", link: "/people" },
        { text: "History", link: "/history" },
        { text: "Ask", link: "/ask" },
        { text: "Current", link: "/current" },
        { text: "Intervention", link: "/human" },
    ];

    // Populate menu
    menuItems.forEach(item => {
        const a = document.createElement("a");
        a.href = item.link;
        a.textContent = item.text;
        contextMenu.appendChild(a);
    });

    // Append to body
    document.body.appendChild(contextMenu);

    // Show menu on right-click
    document.addEventListener("contextmenu", (event) => {
        event.preventDefault();
        contextMenu.style.top = `${event.clientY}px`;
        contextMenu.style.left = `${event.clientX}px`;
        contextMenu.style.display = "block";
    });

    // Hide menu on click
    document.addEventListener("click", () => {
        contextMenu.style.display = "none";
    });
});
