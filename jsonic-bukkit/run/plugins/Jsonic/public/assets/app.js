const msg = document.getElementById("msg");
const out = document.getElementById("out");
const btn = document.getElementById("btn");

msg.textContent = "Statické soubory z /assets/ fungují.";

btn.addEventListener("click", async () => {
    out.textContent = "…";
    try {
        const res = await fetch("/hello");
        const body = await res.text();
        out.textContent = `GET /hello → ${res.status}\n${body}`;
    } catch (err) {
        out.textContent = String(err);
    }
});
