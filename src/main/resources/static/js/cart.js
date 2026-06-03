/**
 * cart.js — Cart operations
 */

let cartData = null;

async function loadCart() {
    if (!isLoggedIn()) {
        document.getElementById('cart-content').innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon"><i class="fa-solid fa-cart-shopping"></i></div>
            <h3>Your cart is empty</h3>
            <p>Please login to view your cart and start shopping</p>
            <a href="/login.html" class="btn btn-primary" style="margin-top:0.5rem">Login to Continue</a>
        </div>`;
        return;
    }

    showCartSkeleton();
    try {
        const res = await api.get('/cart');
        cartData = res.data;
        renderCart(cartData);
        updateCartBadge();
    } catch (e) {
        document.getElementById('cart-content').innerHTML = `<div class="empty-state">
            <p class="text-muted">Error loading cart: ${e.message}</p></div>`;
    }
}

function showCartSkeleton() {
    document.getElementById('cart-content').innerHTML = `
    <div style="display:flex;flex-direction:column;gap:1rem">
        ${Array.from({length:2}, () => `
        <div class="card" style="padding:1.25rem;display:flex;gap:1rem">
            <div class="skeleton" style="width:80px;height:80px;border-radius:var(--radius);flex-shrink:0"></div>
            <div style="flex:1;display:flex;flex-direction:column;gap:0.5rem">
                <div class="skeleton" style="height:14px;width:60%"></div>
                <div class="skeleton" style="height:12px;width:35%"></div>
                <div class="skeleton" style="height:18px;width:25%"></div>
            </div>
        </div>`).join('')}
    </div>`;
}

function renderCart(data) {
    const container = document.getElementById('cart-content');
    const summary = document.getElementById('cart-summary');

    if (!data.items?.length) {
        container.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon"><i class="fa-solid fa-cart-shopping"></i></div>
            <h3>Your cart is empty</h3>
            <p>Start adding fresh products to your cart</p>
            <a href="/products.html" class="btn btn-primary" style="margin-top:0.5rem">
                <i class="fa-solid fa-store"></i> Browse Products
            </a>
        </div>`;
        if (summary) summary.innerHTML = '';
        return;
    }

    container.innerHTML = data.items.map(item => `
    <div class="card animate-fade-in" style="padding:1.25rem;display:flex;gap:1.25rem;align-items:center" id="cart-item-${item.id}">
        <div style="width:80px;height:80px;border-radius:var(--radius);overflow:hidden;flex-shrink:0;background:var(--bg)">
            <img src="${item.productImage || 'https://via.placeholder.com/80'}" alt="${item.productName}"
                 style="width:100%;height:100%;object-fit:cover" onerror="this.src='https://via.placeholder.com/80'">
        </div>
        <div style="flex:1;min-width:0">
            <a href="/product-detail.html?id=${item.productId}" style="font-weight:600;color:var(--text-primary);font-size:0.9rem;display:block;margin-bottom:2px">${item.productName}</a>
            <div style="font-size:0.75rem;color:var(--text-muted)">${item.brand || ''} • ${item.unit || ''}</div>
            <div style="margin-top:0.5rem;font-weight:700;color:var(--primary);font-size:1rem">₹${parseFloat(item.discountPrice || item.price).toFixed(2)}</div>
            ${item.discountPrice ? `<div style="font-size:0.75rem;color:var(--text-muted);text-decoration:line-through">₹${parseFloat(item.price).toFixed(2)}</div>` : ''}
        </div>
        <div style="display:flex;align-items:center;gap:1rem;flex-shrink:0">
            <div class="qty-stepper">
                <button class="qty-btn" onclick="changeQty(${item.id}, ${item.productId}, ${item.quantity - 1})">
                    <i class="fa-solid fa-minus" style="font-size:0.75rem"></i>
                </button>
                <span class="qty-value" id="qty-${item.id}">${item.quantity}</span>
                <button class="qty-btn" onclick="changeQty(${item.id}, ${item.productId}, ${item.quantity + 1})"
                        ${item.quantity >= item.availableStock ? 'disabled' : ''}>
                    <i class="fa-solid fa-plus" style="font-size:0.75rem"></i>
                </button>
            </div>
            <div style="font-weight:700;color:var(--text-primary);min-width:70px;text-align:right">
                ₹${parseFloat(item.itemTotal).toFixed(2)}
            </div>
            <button class="btn-icon btn" style="color:var(--error);background:var(--divider);border:none"
                    onclick="removeItem(${item.id})" title="Remove">
                <i class="fa-solid fa-trash-can" style="font-size:0.8rem"></i>
            </button>
        </div>
    </div>`).join('');

    // Order summary
    const subtotal = data.subtotal || 0;
    const delivery = subtotal > 499 ? 0 : 49;
    const total = subtotal + delivery;

    if (summary) summary.innerHTML = `
    <div class="card" style="padding:1.5rem">
        <h3 style="font-weight:700;margin-bottom:1.25rem">Order Summary</h3>
        <div style="display:flex;flex-direction:column;gap:0.75rem">
            <div class="flex-between" style="font-size:0.875rem">
                <span style="color:var(--text-secondary)">Subtotal (${data.totalItems} items)</span>
                <span style="font-weight:600">₹${parseFloat(subtotal).toFixed(2)}</span>
            </div>
            <div class="flex-between" style="font-size:0.875rem">
                <span style="color:var(--text-secondary)">Delivery Charge</span>
                <span style="font-weight:600;color:${delivery === 0 ? 'var(--primary)' : ''}">
                    ${delivery === 0 ? 'FREE' : '₹' + delivery.toFixed(2)}
                </span>
            </div>
            ${delivery > 0 ? `<div style="background:var(--primary-50);padding:0.5rem 0.75rem;border-radius:var(--radius-sm);font-size:0.8rem;color:var(--primary)">
                <i class="fa-solid fa-truck-fast"></i> Add ₹${(499 - subtotal).toFixed(2)} more for FREE delivery!
            </div>` : `<div style="background:var(--primary-50);padding:0.5rem 0.75rem;border-radius:var(--radius-sm);font-size:0.8rem;color:var(--primary)">
                <i class="fa-solid fa-check-circle"></i> You get FREE delivery!
            </div>`}
            <div class="flex-between" style="font-size:0.875rem">
                <span style="color:var(--text-secondary)">Promo Code</span>
                <span style="color:var(--text-muted)">-</span>
            </div>
        </div>
        <div style="border-top:1px solid var(--divider);margin-top:1rem;padding-top:1rem">
            <div class="flex-between">
                <span style="font-weight:700;font-size:1rem">Total</span>
                <span style="font-weight:800;font-size:1.25rem;color:var(--primary)">₹${(total).toFixed(2)}</span>
            </div>
        </div>
        <div style="margin-top:1rem;display:flex;gap:0.5rem;align-items:center">
            <input type="text" id="promo-code" class="form-control" placeholder="Enter promo code" style="height:38px;font-size:0.875rem">
            <button class="btn btn-ghost btn-sm" onclick="applyPromo()">Apply</button>
        </div>
        <a href="/checkout.html" class="btn btn-primary btn-block btn-lg" style="margin-top:1.25rem">
            <i class="fa-solid fa-lock"></i> Proceed to Checkout
        </a>
        <a href="/products.html" class="btn btn-ghost btn-block" style="margin-top:0.625rem">
            <i class="fa-solid fa-arrow-left"></i> Continue Shopping
        </a>
    </div>`;
}

async function changeQty(itemId, productId, newQty) {
    if (newQty < 1) { removeItem(itemId); return; }
    try {
        const res = await api.put('/cart/update', { productId, quantity: newQty });
        cartData = res.data;
        renderCart(cartData);
        updateCartBadge();
    } catch (e) { showToast(e.message, 'error'); }
}

async function removeItem(itemId) {
    const el = document.getElementById(`cart-item-${itemId}`);
    if (el) { el.style.opacity = '0.4'; el.style.pointerEvents = 'none'; }
    try {
        const res = await api.delete(`/cart/remove/${itemId}`);
        cartData = res.data;
        renderCart(cartData);
        updateCartBadge();
        showToast('Item removed from cart', 'info');
    } catch (e) {
        if (el) { el.style.opacity = ''; el.style.pointerEvents = ''; }
        showToast(e.message, 'error');
    }
}

function applyPromo() {
    showToast('Promo codes not available yet', 'info');
}
