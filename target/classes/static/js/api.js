/**
 * api.js — SwiftCart Centralized API Module
 * - JWT token injection
 * - Error normalization
 * - Shared navbar/footer builder (single source of truth)
 * - Toast, auth helpers, utilities
 */

const API_BASE = 'http://localhost:8080/api';

// ── Core fetch wrapper ──────────────────────────────────────────────────────
async function apiFetch(endpoint, options = {}) {
    const token = localStorage.getItem('token');
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...options.headers
    };
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
        const data = await response.json();
        if (!response.ok) {
            const err = new Error(data.message || data.error || `HTTP ${response.status}`);
            err.status = response.status;
            err.data = data;
            if (response.status === 401) {
                clearAuth();
                if (!window.location.pathname.includes('login')) {
                    showToast('Session expired. Please login again.', 'warning');
                    setTimeout(() => { window.location.href = '/login.html'; }, 1500);
                }
            }
            throw err;
        }
        return data;
    } catch (err) {
        if (err.status) throw err;
        const netErr = new Error('Network error. Is the server running?');
        netErr.isNetworkError = true;
        throw netErr;
    }
}

const api = {
    get:    (ep)       => apiFetch(ep, { method: 'GET' }),
    post:   (ep, body) => apiFetch(ep, { method: 'POST',   body: JSON.stringify(body) }),
    put:    (ep, body) => apiFetch(ep, { method: 'PUT',    body: JSON.stringify(body) }),
    patch:  (ep, body) => apiFetch(ep, { method: 'PATCH',  body: JSON.stringify(body) }),
    delete: (ep)       => apiFetch(ep, { method: 'DELETE' })
};

// ── Auth helpers ──────────────────────────────────────────────────────────
function getToken()    { return localStorage.getItem('token'); }
function isLoggedIn()  { return !!getToken(); }
function getUserInfo() {
    const d = localStorage.getItem('userInfo');
    return d ? JSON.parse(d) : null;
}
function isAdmin() {
    const u = getUserInfo(); return u && u.role === 'ADMIN';
}
function isCustomer() {
    const u = getUserInfo(); return u && u.role === 'CUSTOMER';
}
function clearAuth() {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('cartCount');
}

// ── Route Guards ──────────────────────────────────────────────────────────
function requireAuth(redirect = '/login.html') {
    if (!isLoggedIn()) { window.location.href = redirect; return false; }
    return true;
}
function requireAdmin() {
    if (!isLoggedIn() || !isAdmin()) {
        window.location.href = isLoggedIn() ? '/index.html' : '/login.html';
        return false;
    }
    return true;
}
function requireCustomer() {
    if (!isLoggedIn()) {
        sessionStorage.setItem('redirectAfterLogin', window.location.href);
        window.location.href = '/login.html';
        return false;
    }
    return true;
}
function logout() {
    clearAuth();
    showToast('Logged out successfully', 'success');
    setTimeout(() => window.location.href = '/index.html', 800);
}

// ── Cart badge ────────────────────────────────────────────────────────────
async function updateCartBadge() {
    const badge = document.getElementById('cart-badge');
    
    // Admins don't have a cart, fetching /cart throws 401/403 and crashes their session!
    if (!badge || !isLoggedIn() || isAdmin()) {
        if (badge) badge.style.display = 'none';
        return;
    }
    try {
        const res = await api.get('/cart');
        const count = res.data?.totalItems || 0;
        localStorage.setItem('cartCount', count);
        badge.textContent = count > 99 ? '99+' : count;
        badge.style.display = count > 0 ? 'flex' : 'none';
    } catch (_) {
        const cached = parseInt(localStorage.getItem('cartCount') || '0');
        badge.textContent = cached;
        badge.style.display = cached > 0 ? 'flex' : 'none';
    }
}

// ── Toast system ──────────────────────────────────────────────────────────
function showToast(message, type = 'success', title = '') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const icons = {
        success: 'fa-circle-check',
        error:   'fa-circle-xmark',
        warning: 'fa-triangle-exclamation',
        info:    'fa-circle-info'
    };
    const labels = { success: 'Success', error: 'Error', warning: 'Warning', info: 'Info' };
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <i class="fa-solid ${icons[type]} toast-icon"></i>
        <div class="toast-body">
            <div class="toast-title">${title || labels[type]}</div>
            ${message ? `<div class="toast-message">${message}</div>` : ''}
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <i class="fa-solid fa-xmark"></i>
        </button>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// ── Utility helpers ───────────────────────────────────────────────────────
function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric'
    });
}
function formatDateTime(dateStr) {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}
function debounce(fn, ms = 300) {
    let t;
    return (...args) => { clearTimeout(t); t = setTimeout(() => fn.apply(this, args), ms); };
}
function getStatusBadge(status) {
    return `<span class="badge status-${status}">${status}</span>`;
}
function getStars(rating) {
    const full = Math.floor(rating || 0);
    const half = (rating || 0) - full >= 0.5;
    let stars = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= full) stars += '<i class="fa-solid fa-star" style="color:#FFC107;font-size:0.75rem"></i>';
        else if (i === full + 1 && half) stars += '<i class="fa-solid fa-star-half-stroke" style="color:#FFC107;font-size:0.75rem"></i>';
        else stars += '<i class="fa-regular fa-star" style="color:#DDD;font-size:0.75rem"></i>';
    }
    return stars;
}
function handleNavSearch() {
    const q = document.getElementById('navbar-search-input')?.value.trim();
    if (q) window.location.href = `/products.html?q=${encodeURIComponent(q)}`;
}
function toggleDropdown() {
    document.getElementById('user-dropdown')?.classList.toggle('open');
}
document.addEventListener('click', (e) => {
    if (!e.target.closest('.user-dropdown')) {
        document.getElementById('user-dropdown')?.classList.remove('open');
    }
});

// ══════════════════════════════════════════════════════════════════════════════
//  NAVBAR BUILDER  — single source of truth for all pages
//  Usage: place <div id="navbar-placeholder"></div> at top of <body>,
//  then call buildNavbar() after the api.js <script> tag.
//  index.html is the ONLY page with the announcement bar hardcoded above
//  the placeholder; all other pages just use the placeholder.
// ══════════════════════════════════════════════════════════════════════════════
function buildNavbar() {
    const placeholder = document.getElementById('navbar-placeholder');
    if (!placeholder) return;      // safety: don't inject if no slot

    // If a real <nav class="navbar"> already exists in the DOM, skip injection
    // to prevent duplication. (index.html has its navbar hardcoded without a placeholder.)
    if (document.querySelector('.navbar')) {
        placeholder.remove();
        return;
    }

    const user   = getUserInfo();
    const isAuth = isLoggedIn();

    const rightSection = isAuth ? `
        <a href="/cart.html" class="nav-btn nav-cart-btn" id="nav-cart-link">
            <i class="fa-solid fa-cart-shopping"></i>
            <span style="font-size:0.875rem;font-weight:500">Cart</span>
            <span class="cart-badge" id="cart-badge" style="display:none">0</span>
        </a>
        <div class="user-dropdown">
            <div class="user-avatar-btn" onclick="toggleDropdown()" tabindex="0">
                <div class="user-avatar">${(user?.name || 'U')[0].toUpperCase()}</div>
                <span>${user?.name?.split(' ')[0] || 'User'}</span>
                <i class="fa-solid fa-chevron-down" style="font-size:0.65rem"></i>
            </div>
            <div class="dropdown-menu" id="user-dropdown">
                ${user?.role === 'ADMIN' ? `
                <a href="/admin/dashboard.html" class="dropdown-item">
                    <i class="fa-solid fa-gauge-high"></i> Admin Panel
                </a>
                <hr class="dropdown-divider">
                ` : `
                <a href="/customer/dashboard.html" class="dropdown-item">
                    <i class="fa-solid fa-gauge"></i> Dashboard
                </a>
                <a href="/customer/orders.html" class="dropdown-item">
                    <i class="fa-solid fa-box"></i> My Orders
                </a>
                <a href="/customer/wishlist.html" class="dropdown-item">
                    <i class="fa-regular fa-heart"></i> Wishlist
                </a>
                <a href="/customer/profile.html" class="dropdown-item">
                    <i class="fa-solid fa-user"></i> Profile
                </a>
                <hr class="dropdown-divider">
                `}
                <div class="dropdown-item danger" onclick="logout()">
                    <i class="fa-solid fa-right-from-bracket"></i> Logout
                </div>
            </div>
        </div>` : `
        <a href="/login.html"    class="nav-btn-login">Login</a>
        <a href="/register.html" class="nav-btn-register">Register</a>`;

    const navHTML = `
    <nav class="navbar" id="navbar">
      <div class="navbar-inner">

        <!-- Logo -->
        <a href="/index.html" class="navbar-logo">
          <div class="logo-icon">
            <i class="fa-solid fa-bolt"></i>
          </div>
          <div class="logo-text">Swift<span>Cart</span></div>
        </a>

        <!-- Search -->
        <div class="navbar-search">
          <input type="text" id="navbar-search-input"
                 placeholder="Search groceries, fruits, vegetables..."
                 autocomplete="off">
          <button class="search-btn" onclick="handleNavSearch()" aria-label="Search">
            <i class="fa-solid fa-magnifying-glass"></i>
          </button>
        </div>

        <!-- Actions -->
        <div class="navbar-actions">
          <a href="/products.html" class="nav-btn">
            <i class="fa-solid fa-store"></i>
            <span>Store</span>
          </a>
          ${rightSection}
        </div>

      </div>
    </nav>`;

    placeholder.outerHTML = navHTML;   // replace placeholder with real nav

    // Search on Enter
    document.getElementById('navbar-search-input')
        ?.addEventListener('keydown', e => { if (e.key === 'Enter') handleNavSearch(); });

    // Sticky shadow on scroll
    window.addEventListener('scroll', () => {
        document.getElementById('navbar')?.classList.toggle('scrolled', window.scrollY > 10);
    }, { passive: true });

    if (isAuth) updateCartBadge();
}

// ── Footer builder ────────────────────────────────────────────────────────
function buildFooter() {
    const el = document.getElementById('footer-placeholder');
    if (!el) return;
    el.outerHTML = `
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div>
            <div style="font-family:'Poppins',sans-serif;font-weight:700;font-size:1.5rem;color:white;margin-bottom:12px">
              Swift<span style="color:#FFE082">Cart</span>
            </div>
            <p class="footer-desc">Your premier online grocery destination. Fast delivery, unbeatable prices.</p>
            <div class="footer-social">
              <div class="social-btn"><i class="fa-brands fa-facebook-f"></i></div>
              <div class="social-btn"><i class="fa-brands fa-instagram"></i></div>
              <div class="social-btn"><i class="fa-brands fa-twitter"></i></div>
              <div class="social-btn"><i class="fa-brands fa-youtube"></i></div>
            </div>
          </div>
          <div class="footer-col">
            <h4>Quick Links</h4>
            <div class="footer-links">
              <a href="/index.html">Home</a>
              <a href="/products.html">All Products</a>
              <a href="/products.html?featured=true">Deals &amp; Offers</a>
              <a href="/customer/orders.html">Track Order</a>
            </div>
          </div>
          <div class="footer-col">
            <h4>Categories</h4>
            <div class="footer-links">
              <a href="/products.html?category=1">Fruits &amp; Vegetables</a>
              <a href="/products.html?category=2">Dairy &amp; Eggs</a>
              <a href="/products.html?category=3">Bakery</a>
              <a href="/products.html?category=4">Beverages</a>
            </div>
          </div>
          <div class="footer-col">
            <h4>Support</h4>
            <div class="footer-links">
              <a href="#">Help Center</a>
              <a href="#">Contact Us</a>
              <a href="#">Returns Policy</a>
              <a href="#">Privacy Policy</a>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <span>© 2024 SwiftCart. All rights reserved.</span>
          <span>Made with <i class="fa-solid fa-heart" style="color:#e53935"></i> for fresh groceries</span>
        </div>
      </div>
    </footer>`;
}

// ── Auto-init on DOMContentLoaded ────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    buildNavbar();
    buildFooter();
});
