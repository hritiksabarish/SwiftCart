/**
 * auth.js — Login, Register, Token Management
 */

// ---- LOGIN ----
async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    const email = form.querySelector('#email').value.trim();
    const password = form.querySelector('#password').value;
    const btn = form.querySelector('#login-btn');

    // Validate
    let valid = true;
    if (!email || !/\S+@\S+\.\S+/.test(email)) {
        showFieldError('email-error', 'Please enter a valid email');
        valid = false;
    } else clearFieldError('email-error');

    if (!password) {
        showFieldError('password-error', 'Password is required');
        valid = false;
    } else clearFieldError('password-error');

    if (!valid) return;

    try {
        setLoading(btn, true, 'Logging in...');
        const res = await api.post('/auth/login', { email, password });
        const { token, id, name, email: userEmail, role } = res.data;

        localStorage.setItem('token', token);
        localStorage.setItem('userInfo', JSON.stringify({ id, name, email: userEmail, role }));

        showToast(`Welcome back, ${name}!`, 'success');

        // Redirect
        const redirect = sessionStorage.getItem('redirectAfterLogin');
        sessionStorage.removeItem('redirectAfterLogin');

        setTimeout(() => {
            if (redirect) { window.location.href = redirect; return; }
            if (role === 'ADMIN') window.location.href = '/admin/dashboard.html';
            else window.location.href = '/index.html';
        }, 800);
    } catch (err) {
        showToast(err.message || 'Login failed', 'error');
    } finally {
        setLoading(btn, false, 'Login');
    }
}

// ---- REGISTER ----
async function handleRegister(e) {
    e.preventDefault();
    const form = e.target;
    const name = form.querySelector('#name').value.trim();
    const email = form.querySelector('#email').value.trim();
    const phone = form.querySelector('#phone').value.trim();
    const password = form.querySelector('#password').value;
    const confirmPassword = form.querySelector('#confirm-password').value;
    const btn = form.querySelector('#register-btn');

    // Validate
    let valid = true;
    if (!name || name.length < 2) {
        showFieldError('name-error', 'Name must be at least 2 characters');
        valid = false;
    } else clearFieldError('name-error');

    if (!email || !/\S+@\S+\.\S+/.test(email)) {
        showFieldError('email-error', 'Please enter a valid email');
        valid = false;
    } else clearFieldError('email-error');

    if (!password || password.length < 6) {
        showFieldError('password-error', 'Password must be at least 6 characters');
        valid = false;
    } else clearFieldError('password-error');

    if (password !== confirmPassword) {
        showFieldError('confirm-password-error', 'Passwords do not match');
        valid = false;
    } else clearFieldError('confirm-password-error');

    if (!valid) return;

    try {
        setLoading(btn, true, 'Creating account...');
        const res = await api.post('/auth/register', { name, email, password, phone });
        const { token, id, name: userName, email: userEmail, role } = res.data;

        localStorage.setItem('token', token);
        localStorage.setItem('userInfo', JSON.stringify({ id, name: userName, email: userEmail, role }));

        showToast('Account created successfully! Welcome!', 'success');
        setTimeout(() => window.location.href = '/index.html', 1000);
    } catch (err) {
        showToast(err.message || 'Registration failed', 'error');
    } finally {
        setLoading(btn, false, 'Create Account');
    }
}

// ---- HELPERS ----
function showFieldError(id, msg) {
    const el = document.getElementById(id);
    if (el) { el.textContent = msg; el.style.display = 'flex'; }
}

function clearFieldError(id) {
    const el = document.getElementById(id);
    if (el) { el.textContent = ''; el.style.display = 'none'; }
}

function setLoading(btn, loading, text) {
    if (!btn) return;
    btn.disabled = loading;
    btn.innerHTML = loading
        ? `<span class="spinner spinner-sm"></span> ${text}`
        : text;
}

// Toggle password visibility
function togglePassword(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    if (!input || !icon) return;
    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'fa-solid fa-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'fa-solid fa-eye';
    }
}
