const tg = window.Telegram.WebApp;
const BOT_USERNAME = "Series_tinder_bot";

tg.ready();
tg.expand();

const userId = tg.initDataUnsafe?.user?.id || 111;
let currentSeriesId = null;

const cardElement = document.getElementById('card');
const controlsElement = document.getElementById('controls');
const emptyStateElement = document.getElementById('empty-state');
const seriesTitle = document.getElementById('series-title');
const seriesImage = document.getElementById('series-image');
const seriesDescription = document.getElementById('series-description');

async function loadNextSeries() {
    try {
        // Добавляем заголовок no-cache и параметр времени _t, чтобы обойти кэширование WebKit на iOS
        const response = await fetch(`/api/tinder/next?telegramId=${userId}&_t=${Date.now()}`, {
            headers: {
                'Cache-Control': 'no-cache, no-store, must-revalidate',
                'Pragma': 'no-cache',
                'Expires': '0'
            }
        });

        if (!response.ok || response.status === 204) {
            showEmptyState();
            return;
        }

        const text = await response.text();
        if (!text || text.trim() === "") {
            showEmptyState();
            return;
        }

        const series = JSON.parse(text);
        currentSeriesId = series.id;

        cardElement.classList.remove('show-text');

        seriesTitle.innerText = series.title;
        seriesImage.src = series.imageUrl || '';
        seriesDescription.innerText = series.description || 'Описание отсутствует';

    } catch (error) {
        console.error("Ошибка загрузки данных:", error);
        showEmptyState();
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

        if (result.trim() === "MATCH") {
            tg.showPopup({
                title: "🔥 Мэтч!",
                message: "Вы с партнером хотите посмотреть один и тот же сериал!",
                buttons: [{ type: "ok" }]
            });
            if (tg.HapticFeedback) {
                tg.HapticFeedback.notificationOccurred('success');
            }
        } else if (tg.HapticFeedback) {
            tg.HapticFeedback.impactOccurred('light');
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

cardElement.addEventListener('click', () => {
    cardElement.classList.toggle('show-text');
});

document.getElementById('btn-dislike').addEventListener('click', () => swipe(false));
document.getElementById('btn-like').addEventListener('click', () => swipe(true));

document.getElementById('btn-share').addEventListener('click', () => {
    const deepLink = `https://t.me/${BOT_USERNAME}?start=${userId}`;
    const shareText = "Давай выбирать сериалы вместе! Переходи по ссылке и жми Start 🍿";
    const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(deepLink)}&text=${encodeURIComponent(shareText)}`;
    tg.openTelegramLink(shareUrl);
});

loadNextSeries();