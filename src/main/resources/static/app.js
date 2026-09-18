const tg = window.Telegram.WebApp;
const BOT_USERNAME = "Series_tinder_bot";
tg.expand();

const userId = tg.initDataUnsafe?.user?.id || 111;
let currentSeriesId = null;

const cardElement = document.getElementById('card');
const controlsElement = document.getElementById('controls');
const emptyStateElement = document.getElementById('empty-state');
const seriesTitle = document.getElementById('series-title');
const seriesImage = document.getElementById('series-image');

async function loadNextSeries() {
    try {
        const response = await fetch(`/api/tinder/next?telegramId=${userId}`);

        if (!response.ok || response.status === 204) {
            showEmptyState();
            return;
        }

        const text = await response.text();
        if (!text) {
            showEmptyState();
            return;
        }

        const series = JSON.parse(text);
        currentSeriesId = series.id;
        seriesTitle.innerText = series.title;
        seriesImage.src = series.imageUrl;

    } catch (error) {
        console.error("Ошибка загрузки данных:", error);
    }
}

async function swipe(isLiked) {
    if (!currentSeriesId) return;

    const request = {
        telegramId: userId,
        seriesId: currentSeriesId,
        isLiked: isLiked
    };

    try {
        const response = await fetch('/api/tinder/swipe', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });

        const result = await response.text();

        if (result === "MATCH") {
            tg.showPopup({
                title: "🔥 Мэтч!",
                message: "Вы с партнером хотите посмотреть один и тот же сериал!",
                buttons: [{ type: "ok" }]
            });
        }
        loadNextSeries();

    } catch (error) {
        console.error("Ошибка при отправке свайпа:", error);
    }
}

function showEmptyState() {
    cardElement.classList.add('hidden');
    controlsElement.classList.add('hidden');
    emptyStateElement.classList.remove('hidden');
}

document.getElementById('btn-dislike').addEventListener('click', () => swipe(false));
document.getElementById('btn-like').addEventListener('click', () => swipe(true));

document.getElementById('btn-share').addEventListener('click', () => {
    const deepLink = `https://t.me/${BOT_USERNAME}?start=${userId}`;
    const shareText = "Давай выбирать сериалы вместе! Переходи по ссылке и жми Start 🍿";
    const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(deepLink)}&text=${encodeURIComponent(shareText)}`;
    tg.openTelegramLink(shareUrl);
});

tg.ready();
loadNextSeries();