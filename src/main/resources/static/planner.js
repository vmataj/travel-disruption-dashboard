const $ = id => document.getElementById(id);
const apiRoot = '/api/planner';
const defaultDestination = {
    placeId: 2267057, name: 'Lisbon', region: 'Lisbon District', country: 'Portugal',
    countryCode: 'PT', latitude: 38.72509, longitude: -9.1498, timezone: 'Europe/Lisbon', population: 517802
};
const pages = {
    overview: ['Overview', 'Where to next?', 'Explore cities, check the forecast, and plan your next trip.'],
    destinations: ['Destinations', 'Explore destinations', 'Find a city, check local conditions, and save it for later.'],
    trips: ['Trips', 'Your trips', 'Keep dates, activities, and expenses together.'],
    itinerary: ['Itinerary', 'Trip itinerary', 'Choose a trip to plan its activities.'],
    budget: ['Budget', 'Trip budget', 'Choose a trip to track its expenses.']
};
const categories = { TRANSPORT: 'Transport', LODGING: 'Lodging', FOOD: 'Food', ACTIVITIES: 'Activities', OTHER: 'Other' };
const dateFormat = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' });
const dayFormat = new Intl.DateTimeFormat(undefined, { weekday: 'short', timeZone: 'UTC' });
const favoritesKey = 'travel-planner-guest-favorites';
let account = null;
let trips = [];
let favorites = [];
let destination = defaultDestination;
let selectedId = null;
let forecastRequest;
let searchRequest;
let editorAction;
let page = 'overview';

function element(tag, content, className) {
    const item = document.createElement(tag);
    if (content !== undefined) item.textContent = content;
    if (className) item.className = className;
    return item;
}

function button(label, action, className = 'text-button') {
    const item = element('button', label, className);
    item.type = 'button';
    item.addEventListener('click', action);
    return item;
}

function message(id, content = '') {
    const status = $(id);
    status.textContent = content;
    status.hidden = !content;
}

function date(value) {
    return dateFormat.format(new Date(`${value}T00:00:00Z`));
}

function today() {
    const local = new Date();
    local.setMinutes(local.getMinutes() - local.getTimezoneOffset());
    return local.toISOString().slice(0, 10);
}

function money(amount, currency) {
    return new Intl.NumberFormat(undefined, {
        style: 'currency', currency, maximumFractionDigits: 2, minimumFractionDigits: 2
    }).format(amount);
}

function currentTrip() {
    return trips.find(trip => trip.id === selectedId);
}

async function request(path, { method = 'GET', body, signal } = {}) {
    const headers = { Accept: 'application/json, application/problem+json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET' && account?.csrfHeader) headers[account.csrfHeader] = account.csrfToken;
    const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal });
    if (response.status === 204) return;
    if (response.ok) return response.json();
    const problem = await response.json().catch(() => null);
    if (response.status === 401 || response.status === 403 && !problem?.detail) {
        throw new Error('Your session has expired. Reload and sign in again.');
    }
    throw new Error(problem?.detail || 'The request failed. Try again.');
}

function guestFavorites() {
    try {
        const stored = JSON.parse(localStorage.getItem(favoritesKey) || '[]');
        return Array.isArray(stored) ? stored : [];
    } catch {
        return [];
    }
}

async function loadWorkspace() {
    if (!account?.username) return;
    const workspace = await request(apiRoot);
    trips = workspace.trips;
    favorites = workspace.favorites;
    if (!currentTrip()) selectedId = trips[0]?.id ?? null;
    render();
}

async function initialize() {
    favorites = guestFavorites();
    render();
    selectDestination(defaultDestination);
    try {
        const response = await fetch('/account', { cache: 'no-store' });
        if (response.status === 404) {
            $('guest').hidden = true;
            return;
        }
        if (!response.ok) throw new Error('Could not check your account. Reload to try again.');
        account = await response.json();
        if (account.username) {
            favorites = [];
            render();
            $('guest').hidden = true;
            $('session').hidden = false;
            $('session').elements._csrf.value = account.csrfToken;
            $('username').textContent = account.username;
            await loadWorkspace();
        }
    } catch (error) {
        message('workspace-status', error.message);
    }
}

function navigate(target) {
    if (location.hash === `#${target}`) renderPage();
    else location.hash = target;
}

function renderPage() {
    const key = location.hash.slice(1);
    if (key === 'main') return;
    page = Object.hasOwn(pages, key) ? key : 'overview';
    const [label, title, description] = pages[page];
    $('breadcrumb').textContent = label;
    $('page-title').textContent = title;
    $('page-description').textContent = description;
    document.querySelectorAll('[data-page]').forEach(link => {
        if (link.dataset.page === page) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    });
    const exploring = page === 'overview' || page === 'destinations';
    $('discover').hidden = !exploring;
    $('destination-panels').hidden = !exploring;
    $('overview').hidden = page !== 'overview';
    $('overview-plan').hidden = page !== 'overview';
    $('planning-panels').hidden = exploring;
    $('itinerary').hidden = page === 'budget';
    $('budget').hidden = page === 'itinerary';
}

function stat(icon, value, label) {
    const card = element('div', undefined, 'stat');
    const graphic = element('span', icon);
    graphic.setAttribute('aria-hidden', 'true');
    const text = element('div');
    text.append(element('b', value), element('small', label));
    card.append(graphic, text);
    return card;
}

function row(title, detail, ...actions) {
    const line = element('div', undefined, 'row');
    const text = element('div', undefined, 'row-text');
    text.append(element('strong', title));
    if (detail) text.append(element('small', detail));
    line.append(text);
    if (actions.length) {
        const controls = element('div', undefined, 'row-actions');
        controls.append(...actions);
        line.append(controls);
    }
    return line;
}

function empty(text) {
    return element('p', text, 'empty');
}

function render() {
    renderPage();
    const now = today();
    $('overview').replaceChildren(
        stat('▤', trips.filter(trip => trip.endDate >= now).length, 'Current or upcoming trips'),
        stat('♡', favorites.length, 'Saved destinations'),
        stat('☷', trips.reduce((total, trip) => total + trip.activities.length, 0), 'Planned activities')
    );
    renderFavorites();
    renderTrips();
    renderOverviewTrip(now);
}

function renderOverviewTrip(now) {
    const area = $('overview-trip');
    const trip = trips.find(item => item.endDate >= now);
    if (!trip) {
        area.replaceChildren(empty('No upcoming trips. Choose a destination to start a plan.'));
        return;
    }
    const details = element('div', undefined, 'overview-trip-details');
    const text = element('div');
    text.append(element('strong', trip.title), element('small', `${date(trip.startDate)} – ${date(trip.endDate)}`, 'muted'));
    const next = trip.activities.find(row => row.activity.day >= now)?.activity;
    if (next) text.append(element('small', `Next activity · ${next.title} · ${date(next.day)}`, 'muted'));
    details.append(text, button('Open trip', () => {
        selectedId = trip.id;
        renderTrips();
        navigate('trips');
    }, 'outline'));
    area.replaceChildren(details);
}

function locationLabel(place) {
    return [place.region && place.region !== place.name ? place.region : null, place.country].filter(Boolean).join(', ');
}

function renderFavorites() {
    $('saved-count').textContent = favorites.length;
    const saved = favorites.some(place => place.placeId === destination.placeId);
    $('save-destination').textContent = saved ? '♥ Saved destination' : '♡ Save destination';
    $('save-destination').setAttribute('aria-pressed', String(saved));
    $('favorite-list').replaceChildren(...(favorites.length ? favorites.map(place => row(place.name, locationLabel(place),
        button('View', () => selectDestination(place)),
        button('Remove', () => toggleFavorite(place), 'text-button danger'))) :
        [empty('Save a destination to find it here later.')]));
}

async function toggleFavorite(place) {
    if ($('save-destination').disabled) return;
    const saved = favorites.some(item => item.placeId === place.placeId);
    $('save-destination').disabled = true;
    try {
        if (account?.username) {
            await request(saved ? `${apiRoot}/favorites/${place.placeId}` : `${apiRoot}/favorites`,
                saved ? { method: 'DELETE' } : { method: 'PUT', body: place });
        }
        const updated = saved ? favorites.filter(item => item.placeId !== place.placeId) : [...favorites, place];
        if (!account?.username) localStorage.setItem(favoritesKey, JSON.stringify(updated));
        favorites = updated;
        render();
        message('workspace-status', saved ? `${place.name} removed from saved destinations.` : `${place.name} saved${account?.username ? '' : ' on this browser'}.`);
    } catch (error) {
        message('workspace-status', error.message);
    } finally {
        $('save-destination').disabled = false;
    }
}

function selectDestination(place) {
    destination = place;
    $('destination-name').textContent = place.name;
    $('destination-region').textContent = place.country;
    const meta = [place.region && place.region !== place.name ? place.region : null, place.timezone?.replaceAll('_', ' ')];
    if (place.population) meta.push(`${place.population.toLocaleString()} residents`);
    $('destination-meta').textContent = meta.filter(Boolean).join(' · ');
    const photo = place.placeId === defaultDestination.placeId;
    $('destination-hero').toggleAttribute('data-photo', photo);
    $('photo-credit').hidden = !photo;
    const { latitude: lat, longitude: lon } = place;
    $('map-link').href = `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=12/${lat}/${lon}`;
    $('map-frame').title = `Map of ${place.name}`;
    const bounds = [Math.max(-180, lon - .1), Math.max(-90, lat - .06), Math.min(180, lon + .1), Math.min(90, lat + .06)];
    const mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${bounds.join(',')}&layer=mapnik&marker=${lat},${lon}`;
    if ($('map-frame').getAttribute('src') !== mapUrl) $('map-frame').src = mapUrl;
    renderFavorites();
    showWeather(place);
}

const condition = code => {
    if (code === 0) return ['Clear', '☀'];
    if (code === 1 || code === 2) return ['Partly cloudy', '⛅'];
    if (code === 3) return ['Overcast', '☁'];
    if (code === 45 || code === 48) return ['Fog', '≋'];
    if ([56, 57, 66, 67].includes(code)) return ['Freezing rain', '☂'];
    if ([71, 73, 75, 77, 85, 86].includes(code)) return ['Snow', '❄'];
    if (code >= 95) return ['Thunderstorms', 'ϟ'];
    if (code >= 51) return ['Rain', '☂'];
    return ['Unavailable', '–'];
};

function renderWeather(data) {
    const { current, daily } = data;
    const [label] = condition(current.weather_code);
    const degrees = value => Number.isFinite(value) ? `${Math.round(value)}°` : '—';
    const reading = element('div', undefined, 'reading');
    const readingText = element('span', label);
    readingText.append(element('small', `Feels like ${degrees(current.apparent_temperature)}`, 'muted'));
    reading.append(element('strong', `${degrees(current.temperature_2m)}${Number.isFinite(current.temperature_2m) ? 'C' : ''}`), readingText);
    const forecast = element('div', undefined, 'forecast');
    const watch = [];
    (daily.time || []).forEach((day, index) => {
        const [description, icon] = condition(daily.weather_code?.[index]);
        const high = degrees(daily.temperature_2m_max?.[index]);
        const low = degrees(daily.temperature_2m_min?.[index]);
        const cell = element('div');
        const weekday = dayFormat.format(new Date(`${day}T00:00:00Z`));
        cell.title = `${date(day)}: ${description}, ${high} / ${low}`;
        const symbol = element('span', icon, 'weather-symbol');
        symbol.setAttribute('aria-label', description);
        cell.append(element('b', weekday), symbol, element('span', high), element('span', low, 'muted'));
        forecast.append(cell);
        if (daily.weather_code?.[index] >= 95 || daily.wind_gusts_10m_max?.[index] >= 60) watch.push(weekday);
    });
    const metrics = [];
    if (Number.isFinite(current.relative_humidity_2m)) metrics.push(`${current.relative_humidity_2m}% humidity`);
    if (Number.isFinite(current.wind_speed_10m)) metrics.push(`${Math.round(current.wind_speed_10m)} km/h wind`);
    const content = $('weather-content');
    content.replaceChildren(reading, forecast);
    if (metrics.length) content.append(element('p', metrics.join(' · '), 'weather-note'));
    if (watch.length) content.append(element('p', `Thunderstorms or strong gusts possible ${watch.join(', ')}.`, 'weather-note'));
}

async function showWeather(place) {
    forecastRequest?.abort();
    forecastRequest = new AbortController();
    const { signal } = forecastRequest;
    $('weather-title').textContent = `Weather in ${place.name}`;
    $('weather-content').replaceChildren(empty('Loading the forecast…'));
    try {
        const data = await request(`/api/destinations/weather?lat=${place.latitude}&lon=${place.longitude}`, { signal });
        if (!signal.aborted) renderWeather(data);
    } catch {
        if (!signal.aborted) $('weather-content').replaceChildren(empty('The forecast is unavailable right now.'),
            button('Retry forecast', () => showWeather(place)));
    }
}

$('search').addEventListener('submit', async event => {
    event.preventDefault();
    searchRequest?.abort();
    searchRequest = new AbortController();
    const { signal } = searchRequest;
    const query = event.currentTarget.elements.query.value.trim();
    const results = $('results');
    results.hidden = true;
    results.replaceChildren();
    message('workspace-status');
    if (query.length < 2) {
        message('search-status', 'Enter at least two characters.');
        return;
    }
    message('search-status', 'Searching destinations…');
    try {
        const matches = await request(`/api/destinations?q=${encodeURIComponent(query)}`, { signal });
        if (signal.aborted) return;
        if (!matches.length) {
            message('search-status', 'No destinations found. Try a nearby city or a different spelling.');
            return;
        }
        results.append(...matches.map(place => {
            const choice = button(place.name, () => {
                selectDestination(place);
                results.hidden = true;
                message('search-status');
                $('plan-destination').focus();
            }, '');
            choice.append(element('small', locationLabel(place)));
            return choice;
        }));
        results.hidden = false;
        message('search-status', `${matches.length} ${matches.length === 1 ? 'match' : 'matches'}. Choose a destination.`);
    } catch {
        if (!signal.aborted) message('search-status', 'City search is unavailable. Try again in a moment.');
    }
});

function renderTrips() {
    const trip = currentTrip();
    const signedIn = Boolean(account?.username);
    $('refresh-trips').hidden = !signedIn;
    $('trip-content').hidden = !trip;
    $('trip-list').replaceChildren(...(trips.length ? trips.map(item => {
        const choice = button(item.title, () => {
            selectedId = item.id;
            renderTrips();
            $('trip-title').focus();
        }, 'row-link');
        if (item.id === selectedId) choice.setAttribute('aria-current', 'true');
        choice.append(element('small', `${date(item.startDate)} · ${item.destination.name}${item.owner === account.username ? '' : ' · Shared'}`));
        const wrap = element('div', undefined, 'row');
        wrap.append(choice);
        return wrap;
    }) : [empty(signedIn ? 'Choose a destination to start your first trip.' : 'Sign in to create trips or see trips shared with you.')]));
    if (!trip) return;

    $('trip-title').tabIndex = -1;
    $('trip-title').textContent = trip.title;
    const owner = trip.owner === account.username;
    $('trip-dates').textContent = `${trip.destination.name}, ${trip.destination.country} · ${date(trip.startDate)} – ${date(trip.endDate)}${owner ? '' : ` · Shared by ${trip.owner}`}`;
    $('edit-trip').hidden = !owner;
    $('delete-trip').hidden = !owner;
    $('share-trip').hidden = !owner;
    $('activity-list').replaceChildren(...(trip.activities.length ? trip.activities.map(({ id, activity }) => row(
        activity.title, [`${date(activity.day)}${activity.time ? ` · ${activity.time.slice(0, 5)}` : ''}`, activity.location, activity.notes].filter(Boolean).join('\n'),
        ...(activity.location ? [activityMap(trip, activity.location)] : []),
        button('Edit', () => editActivity(trip, id, activity)),
        button('Remove', () => confirmRemoval(trip, 'activities', id, activity.title), 'text-button danger')
    )) : [empty('Add a place to visit, reservation, or other activity.')]));
    renderBudget(trip);
}

function activityMap(trip, location) {
    const link = element('a', 'Map', 'text-button');
    link.href = 'https://www.openstreetmap.org/search?query=' +
        encodeURIComponent(`${location}, ${trip.destination.name}, ${trip.destination.country}`);
    link.target = '_blank';
    link.rel = 'noreferrer';
    link.setAttribute('aria-label', `Find ${location} on OpenStreetMap`);
    return link;
}

function renderBudget(trip) {
    const categoryTotals = new Map();
    for (const { expense } of trip.expenses) {
        const cents = Math.round(Number(expense.amount) * 100);
        categoryTotals.set(expense.category, (categoryTotals.get(expense.category) ?? 0) + cents);
    }
    const totalCents = [...categoryTotals.values()].reduce((total, cents) => total + cents, 0);
    const limitCents = Math.round(Number(trip.budget) * 100);
    const format = cents => money(cents / 100, trip.currency);
    const summary = element('div', undefined, 'budget-label');
    summary.append(element('strong', format(totalCents)), element('span', limitCents ? `of ${format(limitCents)} planned` : 'No limit set', 'muted'));
    const output = $('budget-summary');
    output.replaceChildren(summary);
    if (limitCents) {
        const meter = element('progress');
        meter.max = limitCents;
        meter.value = Math.min(totalCents, limitCents);
        meter.setAttribute('aria-label', 'Budget used');
        output.append(meter, element('p', totalCents > limitCents ? `${format(totalCents - limitCents)} over budget` :
            `${format(limitCents - totalCents)} remaining`, `empty${totalCents > limitCents ? ' over-budget' : ''}`));
    }
    if (categoryTotals.size) {
        const breakdown = element('ul', undefined, 'budget-categories');
        breakdown.setAttribute('aria-label', 'Spending by category');
        for (const [category, cents] of categoryTotals) {
            breakdown.append(element('li', `${categories[category]} ${format(cents)}`, 'badge'));
        }
        output.append(breakdown);
    }
    $('expense-list').replaceChildren(...(trip.expenses.length ? trip.expenses.map(({ id, expense }) => row(
        expense.title, `${categories[expense.category]} · ${money(expense.amount, trip.currency)}`,
        button('Edit', () => editExpense(trip, id, expense)),
        button('Remove', () => confirmRemoval(trip, 'expenses', id, expense.title), 'text-button danger')
    )) : [empty('Add expenses to see how much you’ve spent.')]));
}

function editor(title, submit, content, action) {
    $('editor-title').textContent = title;
    $('editor-submit').textContent = submit;
    $('editor-fields').replaceChildren(content);
    message('editor-status');
    editorAction = action;
    $('editor').showModal();
}

function formFields(template, values) {
    const fragment = $(`${template}-fields`).content.cloneNode(true);
    for (const [name, value] of Object.entries(values)) {
        const field = fragment.querySelector(`[name="${name}"]`);
        if (field) field.value = value ?? '';
    }
    return fragment;
}

function requireAccount() {
    if (account?.username) return true;
    const hint = element('div');
    hint.append(element('p', 'Sign in to save trips and share them with other travelers.', 'empty'));
    const login = element('a', 'Sign in', 'button outline');
    login.href = '/login.html';
    const signup = element('a', 'Create account', 'button outline');
    signup.href = '/signup.html';
    const actions = element('div', undefined, 'row-actions');
    actions.append(login, signup);
    hint.append(actions);
    editor('Save your trips', 'Done', hint, async () => {});
    return false;
}

function tripUrl(trip) {
    return `${apiRoot}/trips/${trip.id}`;
}

function applyTrip(trip) {
    const position = trips.findIndex(item => item.id === trip.id);
    if (position >= 0) trips[position] = trip;
    else trips.push(trip);
    trips.sort((first, second) => first.startDate.localeCompare(second.startDate));
    selectedId = trip.id;
    render();
}

function editTrip(trip) {
    if (!requireAccount()) return;
    const place = trip?.destination ?? destination;
    const start = today();
    const fields = formFields('trip', trip ?? { title: `Trip to ${place.name}`, startDate: start, endDate: start, budget: 0, currency: 'EUR' });
    const departure = fields.querySelector('[name="startDate"]');
    const arrival = fields.querySelector('[name="endDate"]');
    const setMin = () => {
        arrival.min = departure.value;
        if (arrival.value && arrival.value < departure.value) arrival.value = departure.value;
    };
    departure.addEventListener('change', setMin);
    setMin();
    editor(trip ? 'Edit trip' : `Plan a trip to ${place.name}`, trip ? 'Save changes' : 'Create trip', fields, async body => {
        applyTrip(await request(trip ? tripUrl(trip) : `${apiRoot}/trips`, {
            method: trip ? 'PUT' : 'POST', body: { ...body, destination: place }
        }));
        if (!trip) navigate('trips');
    });
}

function editActivity(trip, id, activity) {
    const fields = formFields('activity', activity ?? { day: trip.startDate });
    const day = fields.querySelector('[name="day"]');
    day.min = trip.startDate;
    day.max = trip.endDate;
    editor(id ? 'Edit activity' : 'Add activity', id ? 'Save changes' : 'Add activity', fields, async body => {
        applyTrip(await request(`${tripUrl(trip)}/activities${id ? `/${id}` : ''}`, {
            method: id ? 'PUT' : 'POST', body: { ...body, time: body.time || null }
        }));
    });
}

function editExpense(trip, id, expense) {
    editor(id ? 'Edit expense' : `Add expense (${trip.currency})`, id ? 'Save changes' : 'Add expense',
        formFields('expense', expense ?? {}), async body => {
            applyTrip(await request(`${tripUrl(trip)}/expenses${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body }));
        });
}

function confirmRemoval(trip, group, id, title) {
    editor('Remove entry?', 'Remove', empty(title), async () => {
        applyTrip(await request(`${tripUrl(trip)}/${group}/${id}`, { method: 'DELETE' }));
    });
}

function sharingFields(trip) {
    const section = element('div', undefined, 'fields');
    section.append(empty('Add someone by username. They can edit the itinerary and expenses from their own account.'));
    const label = element('label', 'Username');
    const field = element('input');
    field.name = 'username';
    field.required = true;
    field.pattern = '[a-z0-9][a-z0-9_\\-]{2,39}';
    field.maxLength = 40;
    field.autocomplete = 'off';
    label.append(field);
    section.append(label);
    if (trip.collaborators.length) {
        const people = element('div', undefined, 'rows');
        people.append(...trip.collaborators.map(username => row(username, 'Can edit', button('Remove access', async event => {
            const control = event.currentTarget;
            control.disabled = true;
            try {
                const updated = await request(`${tripUrl(trip)}/collaborators/${encodeURIComponent(username)}`, { method: 'DELETE' });
                applyTrip(updated);
                $('editor-fields').replaceChildren(sharingFields(updated));
                $('editor-fields').querySelector('input').focus();
                message('editor-status');
            } catch (error) {
                message('editor-status', error.message);
                control.disabled = false;
            }
        }, 'text-button danger'))));
        section.append(people);
    }
    return section;
}

$('editor-form').addEventListener('submit', async event => {
    event.preventDefault();
    const submit = $('editor-submit');
    submit.disabled = true;
    message('editor-status');
    try {
        await editorAction(Object.fromEntries(new FormData(event.currentTarget)));
        $('editor').close();
    } catch (error) {
        message('editor-status', error.message);
    } finally {
        submit.disabled = false;
    }
});
for (const id of ['editor-close', 'editor-cancel']) $(id).addEventListener('click', () => $('editor').close());
$('new-trip').addEventListener('click', () => editTrip());
$('plan-destination').addEventListener('click', () => editTrip());
$('save-destination').addEventListener('click', () => toggleFavorite(destination));
$('edit-trip').addEventListener('click', () => editTrip(currentTrip()));
$('new-activity').addEventListener('click', () => editActivity(currentTrip()));
$('new-expense').addEventListener('click', () => editExpense(currentTrip()));
$('trip-destination').addEventListener('click', () => {
    selectDestination(currentTrip().destination);
    navigate('destinations');
});
$('delete-trip').addEventListener('click', () => {
    const trip = currentTrip();
    editor('Delete trip?', 'Delete trip', empty(`This removes ${trip.title}, its itinerary, and its expenses for everyone with access.`), async () => {
        await request(tripUrl(trip), { method: 'DELETE' });
        trips = trips.filter(item => item.id !== trip.id);
        selectedId = trips[0]?.id ?? null;
        render();
    });
});
$('share-trip').addEventListener('click', () => {
    const trip = currentTrip();
    editor('Share trip', 'Add collaborator', sharingFields(trip), async body => {
        applyTrip(await request(`${tripUrl(trip)}/collaborators`, { method: 'PUT', body }));
        message('workspace-status', `Shared with ${body.username}.`);
    });
});
$('refresh-trips').addEventListener('click', async () => {
    const control = $('refresh-trips');
    control.disabled = true;
    try {
        await loadWorkspace();
        message('workspace-status', 'Trips refreshed.');
    } catch (error) {
        message('workspace-status', error.message);
    } finally {
        control.disabled = false;
    }
});
window.addEventListener('hashchange', () => {
    renderPage();
    if (location.hash !== '#main') window.scrollTo(0, 0);
});
initialize();
