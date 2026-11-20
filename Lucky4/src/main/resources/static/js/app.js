const enterBtn = document.getElementById('enterBtn');
const gameSection = document.getElementById('gameSection');
const bonusSection = document.getElementById('bonusSection');
const teamSection = document.getElementById('teamSection');
const teamGrid = document.getElementById('teamGrid');
const spinBtn = document.getElementById('spinBtn');
const questionBtn = document.getElementById('questionBtn');
const resultText = document.getElementById('resultText');
const profileCard = document.getElementById('profileCard');
const failCard = document.getElementById('failCard');
const failMessage = document.getElementById('failMessage');
const luckValue = document.getElementById('luckValue');
const luckNote = document.getElementById('luckNote');
const streakNote = document.getElementById('streakNote');
const reels = [...document.querySelectorAll('.reel')];

const SPIN_MIN_MS = 1200;    // minimum time the reels spin
const REEL_STAGGER_MS = 320; // delay between each reel stopping
const CELL_MS = 100;         // time for one face to scroll past while spinning (smaller = faster)
const LAND_MS = 900;         // how long a reel takes to slow down and stop
const STRIP_CELLS = 8;       // faces in one loop of the spinning strip

const SPIN_LABELS = ['PRAYING...', 'ASKING THE UNIVERSE...', 'BRIBING THE RNG...', 'BLOWING ON THE MACHINE...'];
const QUESTION_LABELS = ['THINKING...', 'CHOOSING A VICTIM...', 'EXPOSING SOMEONE...'];

let luck = 100;
let lossStreak = 0;
let inGame = false;  // true after ENTER LUCKY 4 is pressed (the machine, team and bonus sections are showing)
let leftTop = false; // true once the person has scrolled down from the top, so the way back up counts as a restart
let dirty = false; // true once something has been spun, peeked at or asked, so scrolling to the top has something to reset
const startingFaces = reels.map(reel => {
    const img = reel.querySelector('img');
    return { src: img ? img.getAttribute('src') : '', alt: img ? img.alt : '' };
});
let spinLabel = 'SPIN!';
let memberImages = ['/images/member1.svg', '/images/member2.svg', '/images/member3.svg', '/images/member4.svg'];

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const pick = list => list[Math.floor(Math.random() * list.length)];

// ---- Luck meter + streak reactions ----
function luckMessage(value) {
    if (value >= 80) return "Lucky streak. Don't tell the machine.";
    if (value >= 40) return 'Mid luck. Mid vibes.';
    if (value > 0) return 'Maybe go outside and touch grass.';
    return 'Luck is at 0%. Please sit down and drink water.';
}

function setLuck(value) {
    luck = Math.max(0, Math.min(100, value));
    luckValue.textContent = luck;
    luckNote.textContent = luckMessage(luck);
}

function updateStreak(won) {
    if (won) {
        lossStreak = 0;
        streakNote.textContent = '';
        return;
    }
    lossStreak += 1;
    if (lossStreak >= 5) {
        streakNote.textContent = `${lossStreak} losses in a row. Maybe the machine is fine and it's you.`;
    } else if (lossStreak >= 3) {
        streakNote.textContent = `${lossStreak} losses in a row. Even the machine feels bad.`;
    } else {
        streakNote.textContent = '';
    }
}

setLuck(100);

enterBtn.addEventListener('click', () => {
    inGame = true;
    leftTop = false;
    gameSection.classList.remove('hidden');
    teamSection.classList.remove('hidden');
    bonusSection.classList.remove('hidden');
    gameSection.scrollIntoView({ behavior: 'smooth' });
});

spinBtn.addEventListener('click', spinGame);

// "Spin again" buttons on the result cards: scroll back to the machine and spin.
document.querySelectorAll('.spin-again').forEach(button => {
    button.addEventListener('click', () => {
        document.querySelector('.machine-shell').scrollIntoView({ behavior: 'smooth', block: 'center' });
        spinGame();
    });
});

// ---- Back to the top = fresh start ----
// Scrolling back up to the top puts the whole page back the way it was when it first opened:
// just the hero and the ENTER button. The machine, team and bonus sections are hidden again and
// the game (cards, reels, luck meter, streak, question) is reset for the next visit.
function restartPage() {
    if (!inGame || spinBtn.disabled) return;   // not entered yet, or the reels are still spinning
    inGame = false;
    leftTop = false;
    dirty = false;

    profileCard.classList.add('hidden');
    failCard.classList.add('hidden');

    reels.forEach((reel, i) => {
        const state = reelState.get(reel);
        if (state) state.loop.cancel();
        settleReel(reel, startingFaces[i].src, startingFaces[i].alt);
    });

    resultText.className = 'result-text';
    resultText.textContent = 'READY WHEN YOU ARE.';
    spinLabel = 'SPIN!';
    spinBtn.textContent = spinLabel;
    lossStreak = 0;
    streakNote.textContent = '';
    setLuck(100);

    document.getElementById('questionNumber').textContent = 'QUESTION #?';
    document.getElementById('questionText').textContent = 'Click the button to expose your friends.';
    questionBtn.textContent = '🎤 GET A QUESTION';

    gameSection.classList.add('hidden');
    teamSection.classList.add('hidden');
    bonusSection.classList.add('hidden');
}

window.addEventListener('scroll', () => {
    if (!inGame) return;
    if (window.scrollY > 200) leftTop = true;                 // they have scrolled down into the game
    else if (leftTop && window.scrollY <= 40) restartPage();   // ...and now they are back at the top
}, { passive: true });

// "Meet the team" / "Ask the group" buttons on the result cards: scroll down to that section.
document.querySelectorAll('.jump-btn').forEach(button => {
    button.addEventListener('click', () => {
        const target = document.getElementById(button.dataset.target);
        if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
});

questionBtn.addEventListener('click', async () => {
    dirty = true;
    questionBtn.disabled = true;
    questionBtn.textContent = pick(QUESTION_LABELS);

    try {
        const response = await fetch('/api/question');
        if (!response.ok) throw new Error('Question request failed');
        const data = await response.json();
        document.getElementById('questionNumber').textContent = `QUESTION #${data.id}`;
        document.getElementById('questionText').textContent = data.question;
    } catch (error) {
        document.getElementById('questionText').textContent = 'Who forgot to connect the Spring API?';
    } finally {
        questionBtn.disabled = false;
        questionBtn.textContent = '🎤 ANOTHER QUESTION? BRAVE.';
    }
});

// ---- Reels: a strip of faces scrolls past the window, then slows down and lands ----
const reelState = new WeakMap(); // reel -> { strip, loop, h, original }

function pickFiller(previous) {
    const choices = memberImages.filter(src => !previous || !previous.endsWith(src));
    const list = choices.length ? choices : memberImages;
    return list[Math.floor(Math.random() * list.length)];
}

function makeCell(src, alt) {
    const img = document.createElement('img');
    img.src = src;
    img.alt = alt || '';
    img.draggable = false;
    return img;
}

function startReel(reel) {
    const img = reel.querySelector('img');
    const h = reel.clientHeight;                       // height of the window
    reel.style.height = reel.offsetHeight + 'px';      // keep the size once the <img> is swapped out

    const strip = document.createElement('div');
    strip.className = 'reel-strip';
    strip.style.setProperty('--cell', h + 'px');

    // first cell = what is showing now; last cell repeats it so the loop has no jump
    const cells = [img.src];
    for (let i = 1; i < STRIP_CELLS; i++) cells.push(pickFiller(cells[i - 1]));
    cells.push(cells[0]);
    cells.forEach(src => strip.appendChild(makeCell(src)));
    reel.replaceChildren(strip);

    const loop = strip.animate(
        [{ transform: 'translateY(0)' }, { transform: `translateY(${-STRIP_CELLS * h}px)` }],
        { duration: STRIP_CELLS * CELL_MS, iterations: Infinity, easing: 'linear' }
    );
    reelState.set(reel, { strip, loop, h, original: { src: img.src, alt: img.alt } });
    reel.classList.remove('landing');
    reel.classList.add('spinning');
}

function stopReel(reel, finalSrc, finalAlt) {
    const state = reelState.get(reel);
    if (!state) return Promise.resolve(settleReel(reel, finalSrc, finalAlt));

    const { strip, loop, h } = state;
    const duration = loop.effect.getComputedTiming().duration;
    const travelled = ((Number(loop.currentTime) || 0) % duration) / duration * STRIP_CELLS * h;
    loop.cancel();

    // Put the final face a few cells ahead, plus one more so the little bounce has something to show.
    const target = Math.max(STRIP_CELLS + 1, Math.ceil(travelled / h) + 3);
    let previous = strip.lastChild.src;
    for (let i = STRIP_CELLS + 1; i <= target + 1; i++) {
        const isFinal = i === target;
        const src = isFinal ? finalSrc : pickFiller(previous);
        strip.appendChild(makeCell(src, isFinal ? finalAlt : ''));
        previous = src;
    }

    reel.classList.add('landing');
    const landing = strip.animate(
        [{ transform: `translateY(${-travelled}px)` }, { transform: `translateY(${-target * h}px)` }],
        { duration: LAND_MS, easing: 'cubic-bezier(.2, .8, .3, 1.12)', fill: 'forwards' }
    );
    return landing.finished.then(() => settleReel(reel, finalSrc, finalAlt)).catch(() => settleReel(reel, finalSrc, finalAlt));
}

function settleReel(reel, src, alt) {
    const img = document.createElement('img');
    img.src = src;
    img.alt = alt;
    reel.replaceChildren(img);
    reel.style.height = '';
    reel.classList.remove('spinning', 'landing');
    reelState.delete(reel);
}

function abortReel(reel) {
    const state = reelState.get(reel);
    if (state) {
        state.loop.cancel();
        settleReel(reel, state.original.src, state.original.alt);
    } else {
        reel.classList.remove('spinning', 'landing');
    }
}

function preloadSpinImages(data) {
    return Promise.all(data.reels.map(member => new Promise(resolve => {
        const img = new Image();
        img.onload = img.onerror = resolve;
        img.src = member.image;
    }))).then(() => data);
}

async function spinGame() {
    if (spinBtn.disabled) return;
    dirty = true;

    spinBtn.disabled = true;
    profileCard.classList.add('hidden');
    failCard.classList.add('hidden');
    resultText.className = 'result-text';
    resultText.textContent = 'SPINNING...';

    // Silly button text while the reels spin.
    let labelIndex = 0;
    spinBtn.textContent = SPIN_LABELS[labelIndex];
    const labelTimer = setInterval(() => {
        labelIndex = (labelIndex + 1) % SPIN_LABELS.length;
        spinBtn.textContent = SPIN_LABELS[labelIndex];
    }, 600);

    // People who prefer less motion just get the result, without the scrolling strip.
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    reels.forEach(reel => {
        if (reduceMotion) reel.classList.add('spinning');
        else startReel(reel);
    });

    try {
        // Ask the server right away and keep the reels spinning for at least
        // SPIN_MIN_MS, so the animation never hides a slow response.
        const [data] = await Promise.all([fetchSpin().then(preloadSpinImages), delay(SPIN_MIN_MS)]);

        // Slow the reels down one at a time, left to right.
        const landings = [];
        for (let i = 0; i < reels.length; i++) {
            const member = data.reels[i];
            landings.push(reduceMotion
                ? Promise.resolve(settleReel(reels[i], member.image, member.name))
                : stopReel(reels[i], member.image, member.name));
            if (i < reels.length - 1) await delay(REEL_STAGGER_MS);
        }
        await Promise.all(landings);

        spinLabel = 'SPIN AGAIN!';

        if (data.match) {
            setLuck(luck + 10);
            updateStreak(true);
            resultText.className = 'result-text success';
            resultText.textContent = data.message;
            showProfile(data.winner);
        } else {
            setLuck(luck - 8);
            updateStreak(false);
            resultText.className = 'result-text fail';
            resultText.textContent = data.message;
            failMessage.textContent = data.message;
            document.getElementById('memeImage').src = data.media;
            failCard.classList.remove('hidden');
            failCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    } catch (error) {
        reels.forEach(abortReel);
        resultText.className = 'result-text fail';
        resultText.textContent = 'Spring Boot did not answer. Check the server.';
    } finally {
        clearInterval(labelTimer);
        spinBtn.textContent = spinLabel;
        spinBtn.disabled = false;
    }
}

async function fetchSpin() {
    const response = await fetch('/api/spin');
    if (!response.ok) throw new Error('Spin request failed');
    return response.json();
}

function showProfile(member) {
    dirty = true;
    document.getElementById('profileImage').src = member.image;
    document.getElementById('profileImage').alt = member.name;
    document.getElementById('profileName').textContent = member.name;
    document.getElementById('profileRole').textContent = member.role;
    document.getElementById('profileCourse').textContent = member.course;
    document.getElementById('profileFavorite').textContent = member.favorite;
    document.getElementById('profileFact').textContent = member.funFact;
    document.getElementById('profileQuote').textContent = member.quote;
    document.getElementById('profilePanic').textContent = member.panicLevel || '-';
    document.getElementById('profileReply').textContent = member.replyTime || '-';
    document.getElementById('profileSuperpower').textContent = member.superpower || '-';
    profileCard.classList.remove('hidden');
    profileCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// ---- Meet the team: built from /api/members ----
async function loadTeam() {
    try {
        const response = await fetch('/api/members');
        if (!response.ok) throw new Error('Members request failed');
        const members = await response.json();
        memberImages = members.map(member => member.image);

        members.forEach(member => {
            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'team-card';

            const img = document.createElement('img');
            img.src = member.image;
            img.alt = member.name;

            const name = document.createElement('strong');
            name.textContent = member.name;

            const role = document.createElement('span');
            role.textContent = member.role;

            card.append(img, name, role);
            card.addEventListener('click', () => {
                resultText.className = 'result-text';
                resultText.textContent = 'Profile opened from the team list.';
                failCard.classList.add('hidden');
                showProfile(member);
            });
            teamGrid.appendChild(card);
        });
    } catch (error) {
        teamGrid.textContent = 'Could not load the team. Is the Spring Boot server running?';
    }
}

loadTeam();

// ---- Secret: click the LUCKY 4 logo 5 times ----
const brand = document.getElementById('brand');
const secret = document.getElementById('secret');
const secretClose = document.getElementById('secretClose');
const SECRET_CAPTIONS = [
    'You found the secret. Nothing happens. Or does it?',
    'Congratulations. You clicked a logo five times.',
    'This is the real jackpot. Please do not tell the machine.',
    'Achievement unlocked: too much free time.'
];
let brandClicks = 0;
let brandTimer;

brand.addEventListener('click', () => {
    brandClicks += 1;
    clearTimeout(brandTimer);
    brandTimer = setTimeout(() => { brandClicks = 0; }, 1500);
    if (brandClicks >= 5) {
        brandClicks = 0;
        document.getElementById('secretImage').src = `/media/meme${1 + Math.floor(Math.random() * 4)}.svg`;
        document.getElementById('secretCaption').textContent = pick(SECRET_CAPTIONS);
        secret.classList.remove('hidden');
        secretClose.focus();
    }
});

function closeSecret() {
    secret.classList.add('hidden');
}
secretClose.addEventListener('click', closeSecret);
secret.addEventListener('click', event => { if (event.target === secret) closeSecret(); });
document.addEventListener('keydown', event => { if (event.key === 'Escape') closeSecret(); });