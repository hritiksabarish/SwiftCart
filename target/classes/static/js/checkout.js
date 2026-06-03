/**
 * checkout.js — 3-step checkout flow
 */

let currentStep = 1;
let selectedAddressId = null;
let cartData = null;
let addresses = [];

async function initCheckout() {
    requireCustomer();
    await Promise.all([loadCart(), loadAddresses()]);
    updateStepUI();
}

async function loadCart() {
    try {
        const res = await api.get('/cart');
        cartData = res.data;
        if (!cartData?.items?.length) {
            showToast('Your cart is empty!', 'warning');
            setTimeout(() => window.location.href = '/cart.html', 1500);
        }
    } catch (e) { showToast('Error loading cart', 'error'); }
}

async function loadAddresses() {
    try {
        const res = await api.get('/addresses');
        addresses = res.data || [];
        renderAddresses();
    } catch (e) { showToast('Error loading addresses', 'error'); }
}

function renderAddresses() {
    const container = document.getElementById('saved-addresses');
    if (!container) return;
    if (!addresses.length) {
        container.innerHTML = `<p class="text-muted" style="font-size:0.875rem">No saved addresses. Add one below.</p>`;
        document.getElementById('add-address-form')?.classList.remove('hidden');
        return;
    }

    container.innerHTML = addresses.map(addr => `
    <label class="custom-check" style="align-items:flex-start;padding:1rem;border:1.5px solid ${selectedAddressId == addr.id ? 'var(--primary-light)' : 'var(--border)'};border-radius:var(--radius);cursor:pointer;margin-bottom:0.625rem;display:flex;gap:0.75rem;transition:border-color 0.2s">
        <input type="radio" name="address" value="${addr.id}" ${selectedAddressId == addr.id || (addr.isDefault && !selectedAddressId) ? 'checked' : ''}
               onchange="selectedAddressId = ${addr.id}; updateAddressBorder(this)">
        <div>
            <div style="display:flex;align-items:center;gap:0.5rem">
                <span style="font-weight:600;font-size:0.875rem">${addr.label || 'Address'}</span>
                ${addr.isDefault ? '<span class="badge badge-primary" style="font-size:0.65rem">DEFAULT</span>' : ''}
            </div>
            <div style="font-size:0.8rem;color:var(--text-secondary);margin-top:0.25rem">${addr.street}, ${addr.city}, ${addr.state} - ${addr.pincode}</div>
            ${addr.landmark ? `<div style="font-size:0.75rem;color:var(--text-muted)">Landmark: ${addr.landmark}</div>` : ''}
        </div>
    </label>`).join('');

    // Auto-select default
    const defaultAddr = addresses.find(a => a.isDefault) || addresses[0];
    if (defaultAddr && !selectedAddressId) selectedAddressId = defaultAddr.id;
    const defaultRadio = document.querySelector(`input[value="${selectedAddressId}"]`);
    if (defaultRadio) defaultRadio.checked = true;
}

function updateAddressBorder(radio) {
    document.querySelectorAll('input[name="address"]').forEach(r => {
        r.closest('label').style.borderColor = r.checked ? 'var(--primary-light)' : 'var(--border)';
    });
}

async function saveNewAddress() {
    const street = document.getElementById('as-street').value.trim();
    const city = document.getElementById('as-city').value.trim();
    const state = document.getElementById('as-state').value.trim();
    const pincode = document.getElementById('as-pincode').value.trim();
    const label = document.getElementById('as-label').value;
    const landmark = document.getElementById('as-landmark').value.trim();

    if (!street || !city || !state || !pincode) { showToast('Please fill all required fields', 'warning'); return; }

    const btn = document.getElementById('save-addr-btn');
    btn.disabled = true; btn.innerHTML = '<span class="spinner spinner-sm"></span> Saving...';
    try {
        await api.post('/addresses', { street, city, state, pincode, label, landmark, isDefault: false });
        showToast('Address saved!', 'success');
        document.getElementById('add-address-form').classList.add('hidden');
        await loadAddresses();
    } catch (e) { showToast(e.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Save Address'; }
}

function renderOrderReview() {
    const container = document.getElementById('order-items-review');
    if (!container || !cartData) return;
    const subtotal = cartData.subtotal || 0;
    const delivery = subtotal > 499 ? 0 : 49;
    container.innerHTML = cartData.items.map(item => `
    <div style="display:flex;gap:1rem;align-items:center;padding:0.75rem 0;border-bottom:1px solid var(--divider)">
        <div style="width:54px;height:54px;border-radius:var(--radius-sm);overflow:hidden;background:var(--bg);flex-shrink:0">
            <img src="${item.productImage || 'https://via.placeholder.com/54'}" alt="" style="width:100%;height:100%;object-fit:cover">
        </div>
        <div style="flex:1;min-width:0">
            <div style="font-weight:600;font-size:0.875rem">${item.productName}</div>
            <div style="font-size:0.75rem;color:var(--text-muted)">Qty: ${item.quantity} × ₹${parseFloat(item.discountPrice || item.price).toFixed(2)}</div>
        </div>
        <div style="font-weight:700;font-size:0.9rem">₹${parseFloat(item.itemTotal).toFixed(2)}</div>
    </div>`).join('') + `
    <div style="margin-top:0.75rem;display:flex;flex-direction:column;gap:0.5rem">
        <div class="flex-between" style="font-size:0.875rem"><span style="color:var(--text-secondary)">Subtotal</span><span>₹${parseFloat(subtotal).toFixed(2)}</span></div>
        <div class="flex-between" style="font-size:0.875rem"><span style="color:var(--text-secondary)">Delivery</span><span style="color:var(--primary)">${delivery === 0 ? 'FREE' : '₹'+delivery}</span></div>
        <div class="flex-between" style="font-weight:700;font-size:1rem;border-top:1px solid var(--divider);padding-top:0.5rem">
            <span>Total</span><span style="color:var(--primary)">₹${(subtotal + delivery).toFixed(2)}</span>
        </div>
    </div>`;

    document.getElementById('delivery-time').textContent =
        new Date(Date.now() + 2*24*60*60*1000).toLocaleDateString('en-IN', {weekday:'long',day:'numeric',month:'short'});
}

function updateStepUI() {
    [1,2,3].forEach(s => {
        const content = document.getElementById(`step-${s}-content`);
        const indicator = document.getElementById(`step-${s}-indicator`);
        if (content) content.classList.toggle('hidden', s !== currentStep);
        if (indicator) {
            indicator.classList.toggle('active', s === currentStep);
            indicator.classList.toggle('done', s < currentStep);
        }
    });
    if (currentStep === 2) renderOrderReview();
}

function nextStep() {
    if (currentStep === 1) {
        const sel = document.querySelector('input[name="address"]:checked');
        if (!sel) { showToast('Please select a delivery address', 'warning'); return; }
        selectedAddressId = parseInt(sel.value);
        currentStep = 2;
    } else if (currentStep === 2) {
        currentStep = 3;
    }
    updateStepUI();
    window.scrollTo(0,0);
}

function prevStep() {
    if (currentStep > 1) { currentStep--; updateStepUI(); }
}

async function placeOrder() {
    const btn = document.getElementById('place-order-btn');
    btn.disabled = true; btn.innerHTML = '<span class="spinner spinner-sm"></span> Placing Order...';
    try {
        const delivery = document.getElementById('delivery-instructions')?.value || '';
        const res = await api.post('/orders', {
            addressId: selectedAddressId,
            paymentMethod: 'CASH_ON_DELIVERY',
            deliveryInstructions: delivery
        });
        const order = res.data;
        updateCartBadge();

        // Show success
        document.getElementById('checkout-steps').classList.add('hidden');
        document.getElementById('order-success').classList.remove('hidden');
        document.getElementById('success-order-id').textContent = '#' + order.id;
        document.getElementById('success-delivery').textContent =
            new Date(Date.now() + 2*24*60*60*1000).toLocaleDateString('en-IN', {weekday:'long',day:'numeric',month:'short'});

        launchConfetti();
    } catch (e) {
        showToast(e.message, 'error');
        btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-check-circle"></i> Place Order';
    }
}

function launchConfetti() {
    const colors = ['#2E7D32','#4CAF50','#FF6F00','#FFC107','#66BB6A'];
    for (let i = 0; i < 80; i++) {
        const el = document.createElement('div');
        el.style.cssText = `position:fixed;top:0;left:${Math.random()*100}%;width:${Math.random()*10+4}px;height:${Math.random()*10+4}px;background:${colors[Math.floor(Math.random()*colors.length)]};border-radius:2px;animation:confettiFall ${Math.random()*2+1}s ease-out forwards;transform:rotate(${Math.random()*360}deg);z-index:9999;pointer-events:none`;
        document.body.appendChild(el);
        setTimeout(() => el.remove(), 3500);
    }
}

const confettiStyle = document.createElement('style');
confettiStyle.textContent = '@keyframes confettiFall{from{top:-10px;opacity:1}to{top:100vh;opacity:0;transform:rotate(720deg)translateX('+Math.random()*200-100+'px)}}';
document.head.appendChild(confettiStyle);
