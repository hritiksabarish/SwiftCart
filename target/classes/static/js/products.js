/**
 * products.js — Product listing, search, filter, pagination
 */

let currentPage = 0;
let totalPages = 0;
let currentParams = {};
let isGridView = true;

async function loadProducts(params = {}) {
    currentParams = params;
    const grid = document.getElementById('products-grid');
    const countEl = document.getElementById('results-count');

    showGridSkeleton();

    const query = buildQuery({ ...params, page: currentPage, size: 12 });

    try {
        let endpoint;
        if (params.q) endpoint = `/products/search?q=${encodeURIComponent(params.q)}&page=${currentPage}&size=12`;
        else if (params.category) endpoint = `/products/category/${params.category}?page=${currentPage}&size=12`;
        else if (params.featured) endpoint = `/products/featured?page=${currentPage}&size=12`;
        else endpoint = `/products?page=${currentPage}&size=12&sortBy=${params.sortBy || 'createdAt'}&sortDir=${params.sortDir || 'desc'}`;

        const res = await api.get(endpoint);
        const data = res.data;
        totalPages = data.totalPages;

        if (countEl) countEl.textContent = `${data.totalElements} results found`;

        if (!data.content?.length) {
            grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1/-1">
                <div class="empty-state-icon"><i class="fa-solid fa-magnifying-glass"></i></div>
                <h3>No products found</h3>
                <p>Try adjusting your search or filters</p>
                <a href="/products.html" class="btn btn-primary btn-sm" style="margin-top:0.5rem">Clear Filters</a>
            </div>`;
            renderPagination();
            return;
        }

        grid.innerHTML = data.content.map(p => buildProductCard(p)).join('');
        renderPagination();
    } catch (e) {
        grid.innerHTML = `<div class="empty-state" style="grid-column: 1/-1">
            <div class="empty-state-icon"><i class="fa-solid fa-circle-exclamation"></i></div>
            <h3>Error loading products</h3>
            <p>${e.message}</p>
        </div>`;
    }
}

function buildQuery(params) {
    return Object.entries(params).filter(([,v]) => v !== undefined && v !== null && v !== '')
        .map(([k,v]) => `${k}=${encodeURIComponent(v)}`).join('&');
}

function showGridSkeleton() {
    const grid = document.getElementById('products-grid');
    grid.innerHTML = Array.from({ length: 8 }, () => `
    <div class="product-card">
        <div class="skeleton" style="aspect-ratio:1"></div>
        <div class="product-card-body">
            <div class="skeleton" style="height:12px;width:40%;margin-bottom:0.5rem"></div>
            <div class="skeleton" style="height:14px;width:70%;margin-bottom:0.375rem"></div>
            <div class="skeleton" style="height:12px;width:50%;margin-bottom:0.5rem"></div>
            <div class="skeleton" style="height:18px;width:40%"></div>
        </div>
    </div>`).join('');
}

function buildProductCard(p) {
    const effectivePrice = p.discountPrice || p.price;
    const discount = p.discountPrice ? Math.round(((p.price - p.discountPrice) / p.price) * 100) : 0;
    return `
    <div class="product-card animate-fade-in">
        <div class="product-card-image">
            <a href="/product-detail.html?id=${p.id}">
                <img src="${p.imageUrl || 'https://via.placeholder.com/300?text=No+Image'}"
                     alt="${p.name}" loading="lazy" onerror="this.src='https://via.placeholder.com/300?text=No+Image'">
            </a>
            ${discount > 0 ? `<div class="product-badge">${discount}% OFF</div>` : ''}
            <button class="product-wishlist-btn" onclick="toggleWishlist(${p.id}, this)">
                <i class="fa-regular fa-heart"></i>
            </button>
        </div>
        <div class="product-card-body">
            <div class="product-brand">${p.brand || ''}</div>
            <a href="/product-detail.html?id=${p.id}" class="product-name">${p.name}</a>
            <div class="product-unit">${p.unit || ''}</div>
            <div class="product-rating">
                ${getStars(p.averageRating)}
                <span class="rating-count">(${p.reviewCount || 0})</span>
            </div>
            <div class="product-price-row">
                <span class="product-price">₹${parseFloat(effectivePrice).toFixed(2)}</span>
                ${p.discountPrice ? `<span class="product-original-price">₹${parseFloat(p.price).toFixed(2)}</span>` : ''}
                ${discount > 0 ? `<span class="product-discount">-${discount}%</span>` : ''}
            </div>
        </div>
        <div class="product-card-footer">
            ${p.stockQuantity > 0
                ? `<button class="btn btn-primary" onclick="addToCart(${p.id})" id="add-cart-${p.id}">
                    <i class="fa-solid fa-cart-plus"></i> Add to Cart</button>`
                : `<div class="out-of-stock-badge"><i class="fa-solid fa-clock"></i> Out of Stock</div>`}
        </div>
    </div>`;
}

function renderPagination() {
    const container = document.getElementById('pagination');
    if (!container) return;
    if (totalPages <= 1) { container.innerHTML = ''; return; }

    let html = `<button class="page-btn" onclick="goToPage(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>
        <i class="fa-solid fa-chevron-left"></i></button>`;

    for (let i = 0; i < totalPages; i++) {
        if (totalPages > 7 && Math.abs(i - currentPage) > 2 && i !== 0 && i !== totalPages - 1) {
            if (i === 1 || i === totalPages - 2) html += `<span class="page-btn" style="border:none;cursor:default">...</span>`;
            continue;
        }
        html += `<button class="page-btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i + 1}</button>`;
    }

    html += `<button class="page-btn" onclick="goToPage(${currentPage + 1})" ${currentPage === totalPages - 1 ? 'disabled' : ''}>
        <i class="fa-solid fa-chevron-right"></i></button>`;

    container.innerHTML = html;
}

function goToPage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    loadProducts(currentParams);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function addToCart(productId) {
    if (!isLoggedIn()) {
        sessionStorage.setItem('redirectAfterLogin', window.location.href);
        window.location.href = '/login.html';
        return;
    }
    const btn = document.getElementById(`add-cart-${productId}`);
    if (!btn) return;
    const orig = btn.innerHTML;
    btn.innerHTML = '<span class="spinner spinner-sm"></span>';
    btn.disabled = true;
    try {
        await api.post('/cart/add', { productId, quantity: 1 });
        showToast('Added to cart!', 'success');
        updateCartBadge();
    } catch (e) { showToast(e.message, 'error'); }
    finally { btn.innerHTML = orig; btn.disabled = false; }
}

async function toggleWishlist(productId, btn) {
    if (!isLoggedIn()) { window.location.href = '/login.html'; return; }
    const active = btn.classList.contains('active');
    try {
        if (active) {
            await api.delete(`/wishlist/${productId}`);
            btn.classList.remove('active');
            btn.innerHTML = '<i class="fa-regular fa-heart"></i>';
        } else {
            await api.post(`/wishlist/${productId}`);
            btn.classList.add('active');
            btn.innerHTML = '<i class="fa-solid fa-heart"></i>';
            showToast('Added to wishlist!', 'success');
        }
    } catch (e) { showToast(e.message, 'error'); }
}

async function loadCategoryFilter() {
    try {
        const res = await api.get('/categories');
        const container = document.getElementById('category-filter');
        if (!container || !res.data) return;
        const params = new URLSearchParams(window.location.search);
        const activeCat = params.get('category');
        container.innerHTML = `
            <div class="custom-check">
                <input type="radio" name="cat" id="cat-all" value="" ${!activeCat ? 'checked' : ''} onchange="applyFilter()">
                <label for="cat-all">All Categories</label>
            </div>` +
            res.data.map(c => `
            <div class="custom-check">
                <input type="radio" name="cat" id="cat-${c.id}" value="${c.id}" ${activeCat == c.id ? 'checked' : ''} onchange="applyFilter()">
                <label for="cat-${c.id}">${c.name}</label>
            </div>`).join('');
    } catch (e) { console.error(e); }
}

function applyFilter() {
    currentPage = 0;
    const catEl = document.querySelector('input[name="cat"]:checked');
    const sortEl = document.getElementById('sort-select');
    const params = {};
    if (catEl?.value) params.category = catEl.value;
    if (sortEl?.value) {
        const [sortBy, sortDir] = sortEl.value.split('-');
        params.sortBy = sortBy;
        params.sortDir = sortDir;
    }
    const searchEl = document.getElementById('search-filter-input');
    if (searchEl?.value.trim()) params.q = searchEl.value.trim();
    loadProducts(params);
}

// Init
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const params = {};
    if (urlParams.get('q')) params.q = urlParams.get('q');
    else if (urlParams.get('category')) params.category = urlParams.get('category');
    else if (urlParams.get('featured')) params.featured = true;

    loadCategoryFilter();
    loadProducts(params);
});
