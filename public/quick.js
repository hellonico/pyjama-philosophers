document.addEventListener("keydown", function (event) {
    // <a href="/chat" className="button">Chat</a>
    // <a href="/people" className="button">People</a>
    // <a href="/ask" className="button">Ask</a>
    // <a href="/current" className="button">Status</a>
    // <a href="/history" className="button">History</a>
    // <a href="/human" className="button">Intervention</a>
    // <!--    <a href="/state" className="button">State</a>-- >

    if (event.ctrlKey) {
        switch (event.key) {
            case "1":
                window.location.href = "/welcome"; // Change URL as needed
                break;
            case "2":
                window.location.href = "/chat";
                break;
            case "3":
                window.location.href = "/people";
                break;
            case "4":
                window.location.href = "/ask";
                break;
            case "5":
                window.location.href = "/current";
                break;
            case "6":
                window.location.href = "/history";
                break;
            case "7":
                window.location.href = "/human";
                break;
            case "0":
                window.location.href = "/state";
                break;
        }
    }
});