const STORAGE_KEY = "jsonic_admin_token";

const state = {
    token: localStorage.getItem(STORAGE_KEY),
    user: null,
    dashboard: null,
    players: [],
    homes: [],
    map: null,
    commands: null,
    page: "dashboard",
};

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

const PAGE_META = {
    dashboard: ["Přehled", "Stav serveru"],
    map: ["Mapa", "Hráči a domovy v top-down pohledu"],
    players: ["Hráči", "Seznam registrovaných postav"],
    homes: ["Domovy", "Uložené souřadnice"],
    console: ["Konsole", "Příkazy serveru (staff)"],
    settings: ["Účet", "Profil a heslo"],
};

async function api(path, { method = "GET", params, auth = true } = {}) {
    let url = path;
    if (params) {
        const q = new URLSearchParams(params);
        url += (url.includes("?") ? "&" : "?") + q.toString();
    }
    const headers = { Accept: "application/json" };
    if (auth && state.token) headers.Authorization = `Bearer ${state.token}`;
    const res = await fetch(url, { method, headers });
    let body = null;
    const ct = res.headers.get("content-type") || "";
    if (ct.includes("json")) {
        try { body = await res.json(); } catch { /* prázdné */ }
    } else {
        body = await res.text();
    }
    if (!res.ok) {
        const msg = body?.error || body?.message || `HTTP ${res.status}`;
        throw new Error(msg);
    }
    return body;
}

const Auth = {
    login: (u, p) => api("/api/auth/login", { method: "POST", params: { username: u, password: p }, auth: false }),
    register: (d) => api("/api/auth/register", { method: "POST", params: d, auth: false }),
    logout: () => api("/api/auth/logout", { method: "POST" }),
    me: () => api("/api/auth/me"),
};

const Data = {
    dashboard: () => api("/api/dashboard"),
    players: () => api("/api/players"),
    homes: () => api("/api/homes"),
    map: () => api("/api/map"),
    commands: () => api("/api/commands"),
    runCommand: (cmd) => api("/api/commands/run", { method: "POST", params: { cmd } }),
    deleteHome: (id) => api(`/api/homes/${encodeURIComponent(id)}`, { method: "DELETE" }),
    updateProfile: (d) => api("/api/auth/profile", { method: "POST", params: d }),
    changePassword: (cur, next) => api("/api/auth/password", { method: "POST", params: { current: cur, next } }),
};

function toast(msg, type = "success") {
    const el = document.createElement("div");
    el.className = `toast toast--${type}`;
    el.textContent = msg;
    $("#toast-root").appendChild(el);
    setTimeout(() => el.remove(), 3500);
}

function showAuthError(msg) {
    const el = $("#auth-error");
    el.textContent = msg;
    el.hidden = false;
    setTimeout(() => { el.hidden = true; }, 5000);
}

function esc(s) {
    return String(s)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function initials(name) {
    return (name || "?").split(/\s+/).map(w => w[0]).join("").slice(0, 2).toUpperCase();
}

function fmtTime(min) {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return h ? `${h}h ${m}m` : `${m}m`;
}

function roleTag(role) {
    const r = (role || "viewer").toLowerCase();
    return `<span class="tag tag--${r}">${r}</span>`;
}

function switchView(name) {
    $$(".view").forEach(v => v.classList.remove("view--active"));
    $(`#view-${name}`).classList.add("view--active");
}

function switchPage(page) {
    state.page = page;
    const [title, sub] = PAGE_META[page] || PAGE_META.dashboard;
    $("#page-title").textContent = title;
    $("#page-subtitle").textContent = sub;
    $$(".nav-item").forEach(n => n.classList.toggle("nav-item--active", n.dataset.page === page));
    $$(".page").forEach(p => p.classList.remove("page--active"));
    $(`#page-${page}`).classList.add("page--active");
    if (page === "map") drawMap();
}

function renderUser() {
    const u = state.user;
    if (!u) return;
    $("#user-name").textContent = u.name || u.username;
    const roleEl = $("#user-role");
    roleEl.textContent = u.role || "viewer";
    roleEl.className = `tag tag--${(u.role || "viewer").toLowerCase()}`;
    $("#user-avatar").textContent = initials(u.name || u.username);
}

function renderDashboard() {
    const d = state.dashboard;
    if (!d) return;
    $("#stats-grid").innerHTML = `
        <article class="stat"><label>Online</label><b>${d.onlinePlayers ?? 0}</b><small>z ${d.totalPlayers ?? 0}</small></article>
        <article class="stat"><label>Domovy</label><b>${d.totalHomes ?? 0}</b></article>
        <article class="stat"><label>Účty</label><b>${d.totalUsers ?? 0}</b></article>
        <article class="stat"><label>Server</label><b style="font-size:1rem">${esc(d.version || "—")}</b></article>
    `;
    const online = d.online || [];
    $("#online-list").innerHTML = online.length
        ? online.map(p => `<li><span>${esc(p.name)}</span><span class="tag tag--online">on</span></li>`).join("")
        : `<li class="empty">Nikdo</li>`;
    const act = d.activity || [];
    $("#activity-list").innerHTML = act.length
        ? act.map(a => `<li><span>${esc(a.text)}</span><span>${esc(a.time)}</span></li>`).join("")
        : `<li class="empty">Prázdný log</li>`;
}

function renderPlayers(filter = "") {
    const q = filter.toLowerCase();
    const mode = $("#players-filter")?.value || "all";
    let list = [...state.players];
    if (q) list = list.filter(p => p.name.toLowerCase().includes(q) || (p.uuid || "").includes(q));
    if (mode === "online") list = list.filter(p => p.online);
    if (mode === "offline") list = list.filter(p => !p.online);
    const tbody = $("#players-tbody");
    if (!list.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty">Nic nenalezeno</td></tr>`;
        return;
    }
    tbody.innerHTML = list.map(p => `
        <tr>
            <td><div class="player-name"><strong>${esc(p.name)}</strong><code>${esc(p.uuid || "")}</code></div></td>
            <td><span class="tag tag--${p.online ? "online" : "offline"}">${p.online ? "on" : "off"}</span></td>
            <td>${fmtTime(p.playtime || 0)}</td>
            <td>${esc(p.lastSeen || "—")}</td>
            <td>${roleTag(p.role)}</td>
        </tr>
    `).join("");
}

function renderHomes(filter = "") {
    const q = filter.toLowerCase();
    let list = [...state.homes];
    if (q) {
        list = list.filter(h =>
            h.id.toLowerCase().includes(q) ||
            (h.name || "").toLowerCase().includes(q) ||
            h.owner.toLowerCase().includes(q) ||
            h.world.toLowerCase().includes(q)
        );
    }
    const grid = $("#homes-grid");
    if (!list.length) {
        grid.innerHTML = `<div class="empty">Žádné domovy</div>`;
        return;
    }
    const canDel = ["admin", "moderator"].includes(state.user?.role);
    grid.innerHTML = list.map(h => `
        <article class="card">
            <div class="card-head">
                <h4>${esc(h.name || h.id)}</h4>
                <span class="tag tag--${h.public ? "public" : "private"}">${h.public ? "veřejný" : "soukromý"}</span>
            </div>
            <dl class="card-dl">
                <div><dt>vlastník</dt><dd>${esc(h.owner)}</dd></div>
                <div><dt>svět</dt><dd>${esc(h.world)}</dd></div>
                <div><dt>xyz</dt><dd>${h.x} ${h.y} ${h.z}</dd></div>
            </dl>
            <div class="card-actions">
                <button type="button" class="btn btn--ghost btn--sm" data-goto-map="${esc(h.id)}">mapa</button>
                ${canDel ? `<button type="button" class="btn btn--danger btn--sm" data-del-home="${esc(h.id)}">smazat</button>` : ""}
            </div>
        </article>
    `).join("");
}

function renderCommands() {
    const c = state.commands;
    if (!c) return;
    const hist = c.history || [];
    $("#cmd-history").innerHTML = hist.length
        ? [...hist].reverse().map(h =>
            `<li><span>${esc(h.user)}: ${esc(h.cmd)}</span><span>${esc(h.time)}</span></li>`
        ).join("")
        : `<li class="empty">Zatím nic</li>`;
    const can = c.canRun && ["admin", "moderator"].includes(state.user?.role);
    $("#console-input").disabled = !can;
    $("#form-console button").disabled = !can;
    $$("#quick-cmds button").forEach(b => { b.disabled = !can; });
    $("#console-hint").textContent = can
        ? "Příkaz se spustí v konzoli serveru. Opatrně."
        : "Spouštění příkazů je jen pro admina a moderátora.";
}

/* ── Mapa (canvas top-down) ─────────────────────────── */

const mapState = { scale: 1, ox: 0, oy: 0, hover: null };

function mapMarkersForWorld(world) {
    if (!state.map) return [];
    const showH = $("#map-show-homes")?.checked !== false;
    const showP = $("#map-show-players")?.checked !== false;
    return (state.map.markers || []).filter(m => {
        if (m.world !== world) return false;
        if (m.type === "home" && !showH) return false;
        if (m.type === "player" && !showP) return false;
        return true;
    });
}

function drawMap() {
    const canvas = $("#map-canvas");
    if (!canvas || !state.map) return;
    const ctx = canvas.getContext("2d");
    const world = $("#map-world")?.value || state.map.worlds?.[0] || "world";
    const markers = mapMarkersForWorld(world);
    const W = canvas.width;
    const H = canvas.height;

    ctx.fillStyle = "#0d120d";
    ctx.fillRect(0, 0, W, H);

    if (!markers.length) {
        ctx.fillStyle = "#666";
        ctx.font = "14px system-ui";
        ctx.textAlign = "center";
        ctx.fillText("V tomhle světě nic k zobrazení", W / 2, H / 2);
        return;
    }

    const xs = markers.map(m => m.x);
    const zs = markers.map(m => m.z);
    const pad = 80;
    const minX = Math.min(...xs) - pad;
    const maxX = Math.max(...xs) + pad;
    const minZ = Math.min(...zs) - pad;
    const maxZ = Math.max(...zs) + pad;
    const sx = (W - 40) / (maxX - minX || 1);
    const sz = (H - 40) / (maxZ - minZ || 1);
    const s = Math.min(sx, sz);
    const ox = 20 - minX * s + (W - 40 - (maxX - minX) * s) / 2;
    const oy = 20 - minZ * s + (H - 40 - (maxZ - minZ) * s) / 2;

    mapState.scale = s;
    mapState.ox = ox;
    mapState.oy = oy;

    ctx.strokeStyle = "#1e2e1e";
    ctx.lineWidth = 1;
    const step = 64;
    for (let gx = Math.floor(minX / step) * step; gx <= maxX; gx += step) {
        const px = gx * s + ox;
        ctx.beginPath();
        ctx.moveTo(px, 0);
        ctx.lineTo(px, H);
        ctx.stroke();
    }
    for (let gz = Math.floor(minZ / step) * step; gz <= maxZ; gz += step) {
        const py = gz * s + oy;
        ctx.beginPath();
        ctx.moveTo(0, py);
        ctx.lineTo(W, py);
        ctx.stroke();
    }

    ctx.strokeStyle = "#6a9920";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(0 * s + ox, 0 * s + oy, 4, 0, Math.PI * 2);
    ctx.stroke();

    for (const m of markers) {
        const px = m.x * s + ox;
        const py = m.z * s + oy;
        if (m.type === "player") {
            ctx.fillStyle = "#8ec926";
            ctx.beginPath();
            ctx.arc(px, py, 6, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = "#111";
            ctx.lineWidth = 1;
            ctx.stroke();
        } else {
            ctx.fillStyle = "#5a8fd4";
            ctx.fillRect(px - 5, py - 5, 10, 10);
        }
        ctx.fillStyle = "#ccc";
        ctx.font = "11px ui-monospace, monospace";
        ctx.textAlign = "left";
        ctx.fillText(m.label, px + 8, py - 4);
    }

    ctx.fillStyle = "#555";
    ctx.font = "11px system-ui";
    ctx.textAlign = "left";
    ctx.fillText(`${world} · ${markers.length} bodů · mřížka 64`, 8, H - 8);
}

function setupMapControls() {
    const sel = $("#map-world");
    if (!sel || !state.map) return;
    const fromApi = (state.map.worlds || []).filter(w => w && w !== "null");
    const fromMarkers = [...new Set((state.map.markers || []).map(m => m.world).filter(Boolean))];
    const worlds = fromApi.length ? fromApi : (fromMarkers.length ? fromMarkers : ["world"]);
    sel.innerHTML = worlds.map(w => `<option value="${esc(w)}">${esc(w)}</option>`).join("");
    sel.onchange = () => drawMap();
    $("#map-show-homes")?.addEventListener("change", drawMap);
    $("#map-show-players")?.addEventListener("change", drawMap);

    const canvas = $("#map-canvas");
    const tip = $("#map-tooltip");
    canvas?.addEventListener("mousemove", e => {
        const rect = canvas.getBoundingClientRect();
        const cx = (e.clientX - rect.left) * (canvas.width / rect.width);
        const cy = (e.clientY - rect.top) * (canvas.height / rect.height);
        const world = sel.value;
        const s = mapState.scale;
        const ox = mapState.ox;
        const oy = mapState.oy;
        let hit = null;
        for (const m of mapMarkersForWorld(world)) {
            const px = m.x * s + ox;
            const py = m.z * s + oy;
            if (Math.hypot(cx - px, cy - py) < 10) { hit = m; break; }
        }
        if (hit && tip) {
            tip.hidden = false;
            tip.style.left = `${e.clientX - rect.left + 12}px`;
            tip.style.top = `${e.clientY - rect.top + 12}px`;
            tip.textContent = `${hit.label} · ${hit.x}, ${hit.y}, ${hit.z}`;
        } else if (tip) {
            tip.hidden = true;
        }
    });
    canvas?.addEventListener("click", e => {
        const rect = canvas.getBoundingClientRect();
        const cx = (e.clientX - rect.left) * (canvas.width / rect.width);
        const cy = (e.clientY - rect.top) * (canvas.height / rect.height);
        const bx = Math.round((cx - mapState.ox) / mapState.scale);
        const bz = Math.round((cy - mapState.oy) / mapState.scale);
        $("#map-hint").textContent = `Klik: ${bx}, ?, ${bz}`;
    });
}

function fillSettingsForm() {
    const u = state.user;
    if (!u) return;
    const f = $("#form-profile");
    f.name.value = u.name || "";
    f.email.value = u.email || "";
}

async function loadAll() {
    try {
        const [dash, players, homes, map, commands] = await Promise.all([
            Data.dashboard(),
            Data.players(),
            Data.homes(),
            Data.map(),
            Data.commands(),
        ]);
        state.dashboard = dash;
        state.players = players.players || players || [];
        state.homes = homes.homes || homes || [];
        state.map = map;
        state.commands = commands;
        renderDashboard();
        renderPlayers($("#players-search")?.value || "");
        renderHomes($("#homes-search")?.value || "");
        renderCommands();
        setupMapControls();
        if (state.page === "map") drawMap();
    } catch (e) {
        if (String(e.message).includes("401") || String(e.message).includes("invalid_token")) {
            logout(false);
            return;
        }
        toast(e.message, "error");
    }
}

async function bootstrap() {
    if (!state.token) {
        switchView("auth");
        return;
    }
    try {
        state.user = await Auth.me();
        switchView("app");
        renderUser();
        fillSettingsForm();
        await loadAll();
    } catch {
        logout(false);
    }
}

async function login(username, password) {
    const res = await Auth.login(username, password);
    state.token = res.token;
    state.user = res.user;
    localStorage.setItem(STORAGE_KEY, state.token);
    switchView("app");
    renderUser();
    fillSettingsForm();
    toast(`Ahoj, ${state.user.name || state.user.username}`);
    await loadAll();
}

async function register(data) {
    const res = await Auth.register(data);
    state.token = res.token;
    state.user = res.user;
    localStorage.setItem(STORAGE_KEY, state.token);
    switchView("app");
    renderUser();
    toast("Účet hotový");
    await loadAll();
}

async function logout(callApi = true) {
    if (callApi && state.token) {
        try { await Auth.logout(); } catch { /* ignore */ }
    }
    state.token = null;
    state.user = null;
    localStorage.removeItem(STORAGE_KEY);
    switchView("auth");
}

async function runConsoleCommand(cmd) {
    if (!cmd.trim()) return;
    try {
        await Data.runCommand(cmd.trim());
        toast(`> ${cmd.trim()}`);
        $("#console-input").value = "";
        state.commands = await Data.commands();
        renderCommands();
        await loadAll();
    } catch (e) {
        toast(e.message, "error");
    }
}

$$("[data-auth-tab]").forEach(tab => {
    tab.addEventListener("click", () => {
        $$("[data-auth-tab]").forEach(t => t.classList.toggle("auth-tab--active", t === tab));
        const target = tab.dataset.authTab;
        $("#form-login").classList.toggle("auth-form--active", target === "login");
        $("#form-register").classList.toggle("auth-form--active", target === "register");
        $("#auth-error").hidden = true;
    });
});

$("#form-login").addEventListener("submit", async e => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
        await login(fd.get("username"), fd.get("password"));
    } catch (err) {
        showAuthError(err.message || "Chyba přihlášení");
    }
});

$("#form-register").addEventListener("submit", async e => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
        await register({
            username: fd.get("username"),
            password: fd.get("password"),
            name: fd.get("name"),
            email: fd.get("email"),
        });
    } catch (err) {
        showAuthError(err.message || "Registrace selhala");
    }
});

$("#btn-logout").addEventListener("click", () => logout());
$$(".nav-item").forEach(btn => btn.addEventListener("click", () => switchPage(btn.dataset.page)));
$("#btn-refresh").addEventListener("click", () => { loadAll(); toast("Obnoveno"); });

$("#players-search")?.addEventListener("input", e => renderPlayers(e.target.value));
$("#players-filter")?.addEventListener("change", () => renderPlayers($("#players-search").value));
$("#homes-search")?.addEventListener("input", e => renderHomes(e.target.value));

$("#homes-grid")?.addEventListener("click", async e => {
    const del = e.target.closest("[data-del-home]");
    if (del) {
        if (!confirm("Smazat domov?")) return;
        try {
            await Data.deleteHome(del.dataset.delHome);
            toast("Smazáno");
            await loadAll();
        } catch (err) {
            toast(err.message, "error");
        }
        return;
    }
    const mapBtn = e.target.closest("[data-goto-map]");
    if (mapBtn) {
        const home = state.homes.find(h => h.id === mapBtn.dataset.gotoMap);
        if (home && $("#map-world")) {
            $("#map-world").value = home.world;
            switchPage("map");
            drawMap();
        }
    }
});

$("#form-console")?.addEventListener("submit", async e => {
    e.preventDefault();
    await runConsoleCommand($("#console-input").value);
});

$("#quick-cmds")?.addEventListener("click", e => {
    const btn = e.target.closest("[data-cmd]");
    if (btn) runConsoleCommand(btn.dataset.cmd);
});

$("#form-profile")?.addEventListener("submit", async e => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
        const res = await Data.updateProfile({ name: fd.get("name"), email: fd.get("email") });
        state.user = res.user || state.user;
        renderUser();
        toast("Uloženo");
    } catch (err) {
        toast(err.message, "error");
    }
});

$("#form-password")?.addEventListener("submit", async e => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
        await Data.changePassword(fd.get("current"), fd.get("next"));
        toast("Heslo změněno");
        e.target.reset();
    } catch (err) {
        toast(err.message, "error");
    }
});

bootstrap();
