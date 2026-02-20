let globalWinners = [];
let globalCurrentWinner = -1;
let globalCandidates = [];

// ── Confetti ──────────────────────────────────────────────────────────────────

const CONFETTI_COLORS = ['#e2001a', '#58a6ff', '#f0f6fc', '#ffd700', '#ff6b6b', '#00d4aa'];
const CONFETTI_DURATION_MS = 3500;

function launchConfetti() {
    const canvas = document.getElementById('confetti-canvas');
    const ctx = canvas.getContext('2d');

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const particles = Array.from({ length: 160 }, () => ({
        x: Math.random() * canvas.width,
        y: -20 - Math.random() * 120,
        w: 8 + Math.random() * 8,
        h: 4 + Math.random() * 4,
        color: CONFETTI_COLORS[Math.floor(Math.random() * CONFETTI_COLORS.length)],
        vx: (Math.random() - 0.5) * 4,
        vy: 3 + Math.random() * 4,
        angle: Math.random() * Math.PI * 2,
        spin: (Math.random() - 0.5) * 0.2,
        opacity: 1,
    }));

    let frame;
    const endTime = Date.now() + CONFETTI_DURATION_MS;

    function draw() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        const now = Date.now();
        const remaining = endTime - now;

        particles.forEach(p => {
            p.x += p.vx;
            p.y += p.vy;
            p.angle += p.spin;
            p.vy += 0.07; // gravity
            if (remaining < 800) p.opacity = Math.max(0, remaining / 800);

            ctx.save();
            ctx.globalAlpha = p.opacity;
            ctx.translate(p.x, p.y);
            ctx.rotate(p.angle);
            ctx.fillStyle = p.color;
            ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
            ctx.restore();
        });

        if (now < endTime) {
            frame = requestAnimationFrame(draw);
        } else {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    }

    if (frame) cancelAnimationFrame(frame);
    draw();
}

// ── Countdown ─────────────────────────────────────────────────────────────────

/**
 * Runs a 3-second countdown overlay and returns a Promise that resolves when done.
 */
function runCountdown() {
    return new Promise(resolve => {
        const overlay = document.getElementById('countdown-overlay');
        const numberEl = document.getElementById('countdown-number');
        let count = 3;

        numberEl.textContent = count;
        overlay.classList.add('active');

        function tick() {
            numberEl.classList.add('pulse');
            setTimeout(() => numberEl.classList.remove('pulse'), 150);

            if (count <= 0) {
                overlay.classList.remove('active');
                resolve();
                return;
            }
            numberEl.textContent = count;
            count--;
            setTimeout(tick, 1000);
        }
        tick();
    });
}

// ── Slot machine ──────────────────────────────────────────────────────────────

/**
 * Shows the candidates slot-machine panel, cycles through names, then lands on
 * the winner. Returns a Promise that resolves once the winner is shown briefly.
 */
function runSlotMachine(candidates, winner) {
    return new Promise(resolve => {
        const panel = document.getElementById('candidates');
        const slotName = document.getElementById('slot-name');
        const slotHandle = document.getElementById('slot-handle');
        const slotMachine = document.getElementById('slot-machine');

        // Show candidates panel, hide others
        document.getElementById('home').classList.add('hidden');
        document.getElementById('winner').classList.remove('visible');
        panel.classList.remove('visible');
        void panel.offsetWidth; // force reflow for animation
        panel.classList.add('visible');

        // If no candidates to cycle, skip animation
        const pool = candidates && candidates.length > 0 ? candidates : [winner];
        // Exclude the winner from the cycling pool to avoid revealing early.
        // If there's only one candidate (the winner), they will appear during cycling — that's acceptable.
        const others = pool.filter(c => c.screenName !== winner.screenName);
        const cyclePool = others.length > 0 ? others : pool;

        const startTime = Date.now();
        const duration = 3500; // ms of cycling before landing

        function cycle() {
            const elapsed = Date.now() - startTime;
            if (elapsed < duration) {
                // Exponential slow-down: delay grows from min 80ms to max 600ms (80 + 520)
                const progress = elapsed / duration;
                const delay = Math.round(80 + Math.pow(progress, 2) * 520);

                const c = cyclePool[Math.floor(Math.random() * cyclePool.length)];
                slotName.textContent = c.name || c.screenName;
                slotHandle.textContent = '@' + c.screenName;

                setTimeout(cycle, delay);
            } else {
                // Land on winner
                slotName.textContent = winner.name || winner.screenName;
                slotHandle.textContent = '@' + winner.screenName;
                slotMachine.classList.add('locked');

                // Hold for 1.5 s, then hide panel and resolve
                setTimeout(() => {
                    panel.classList.remove('visible');
                    slotMachine.classList.remove('locked');
                    resolve();
                }, 1500);
            }
        }
        cycle();
    });
}

// ── Raffle logic ──────────────────────────────────────────────────────────────

function performRaffle() {
    const speaker = document.getElementById('speaker').value;
    const btn = document.getElementById('btn-raffle');
    btn.disabled = true;

    // Start fetching candidates + raffle concurrently with the countdown
    const fetchRaffle = fetch("/raffle?speaker=" + encodeURIComponent(speaker))
        .then(r => r.json());
    const fetchCandidates = fetch("/candidates?speaker=" + encodeURIComponent(speaker))
        .then(r => r.json())
        .catch(() => []); // graceful fallback if endpoint unavailable

    Promise.all([runCountdown(), fetchRaffle, fetchCandidates])
        .then(([, winners, candidates]) => {
            btn.disabled = false;
            globalWinners = winners;
            globalCurrentWinner = -1;
            // Use candidates list; fall back to winners list if empty
            globalCandidates = candidates.length > 0 ? candidates : winners;
            showNextWinner();
        })
        .catch(() => {
            btn.disabled = false;
        });
}

/**
 * Called by the "next winner" button — runs countdown first, then reveals.
 */
function nextWinner() {
    runCountdown().then(() => showNextWinner());
}

/**
 * Advances to the next winner: shows the slot-machine then the winner panel.
 */
function showNextWinner() {
    globalCurrentWinner++;
    if (globalCurrentWinner >= globalWinners.length) {
        showNoWinnerFound();
        return;
    }

    const winner = globalWinners[globalCurrentWinner];

    runSlotMachine(globalCandidates, winner).then(() => {
        displayWinner(winner);
    });
}

function displayWinner(winner) {
    const postUrl = winner.postUrl;

    const winnerEl = document.getElementById('winner');
    winnerEl.classList.remove('visible');
    void winnerEl.offsetWidth; // force reflow so animation replays
    winnerEl.classList.add('visible');

    document.getElementById('winner-name').innerHTML =
        winner.name + ' (<cite>@' + escapeHtml(winner.screenName) + '</cite>)';

    const postElement = document.getElementById('post');
    const useOEmbed = document.getElementById('use-oembed').checked;

    launchConfetti();

    if (useOEmbed) {
        postElement.innerHTML = '<p style="color:var(--text-secondary);text-align:center;padding:1rem;">Chargement du post…</p>';

        fetch("/embed?url=" + encodeURIComponent(postUrl))
            .then(r => r.json())
            .then(embedData => {
                if (embedData.html) {
                    let displayHtml = '';
                    if (winner.imageUrl) {
                        displayHtml += '<img src="' + escapeHtml(winner.imageUrl) +
                            '" alt="Post image" style="max-width:100%;height:auto;border-radius:8px;margin-bottom:10px;" ' +
                            'onerror="this.style.display=\'none\'">';
                    }
                    displayHtml += embedData.html;
                    postElement.innerHTML = displayHtml;
                } else {
                    displayCustomPost(postElement, winner, postUrl);
                }
            })
            .catch(() => displayCustomPost(postElement, winner, postUrl));
    } else {
        displayCustomPost(postElement, winner, postUrl);
    }
}

function displayCustomPost(postElement, winner, postUrl) {
    let html = '<div style="border:1px solid var(--border);padding:1.25rem;border-radius:10px;background:var(--card-bg);">';

    if (winner.postText) {
        html += '<p style="font-size:1.1rem;line-height:1.6;margin-bottom:0.75rem;color:var(--text-primary);">' +
            escapeHtml(winner.postText) + '</p>';
    }

    if (winner.imageUrl) {
        html += '<img src="' + escapeHtml(winner.imageUrl) +
            '" alt="Post image" style="max-width:100%;height:auto;border-radius:8px;margin-bottom:0.75rem;" ' +
            'onerror="this.style.display=\'none\'">';
    }

    html += '<a href="' + escapeHtml(postUrl) +
        '" target="_blank" style="color:var(--accent);text-decoration:none;font-size:0.9rem;">Voir sur Bluesky &#x2192;</a>';
    html += '</div>';

    postElement.innerHTML = html;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNoWinnerFound() {
    const winnerEl = document.getElementById('winner');
    winnerEl.classList.remove('visible');
    void winnerEl.offsetWidth;
    winnerEl.classList.add('visible');
    document.getElementById('winner-name').innerHTML = '';
    document.getElementById('post').innerHTML =
        '<p style="color:var(--text-secondary);text-align:center;padding:2rem;">Aucun gagnant trouv&eacute;.</p>';
}
