// login.js - Simplified and Robust for Servlet Submission
const profileKey = "civicUserProfile";

document.addEventListener('DOMContentLoaded', () => {
    const signUpButton = document.getElementById("signUp");
    const signInButton = document.getElementById("signIn");
    const container = document.getElementById("container");
    const signInForm = document.getElementById("signInForm");
    const signUpForm = document.getElementById("signUpForm");

    if (signUpButton && container) {
        signUpButton.addEventListener("click", () => container.classList.add("right-panel-active"));
    }

    if (signInButton && container) {
        signInButton.addEventListener("click", () => container.classList.remove("right-panel-active"));
    }

    document.querySelectorAll('.toggle-password').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');
            const input = document.getElementById(targetId);
            if (!input) return;
            const isPassword = input.type === 'password';
            input.type = isPassword ? 'text' : 'password';
            btn.innerHTML = isPassword ? '<i class="fas fa-eye-slash"></i>' : '<i class="fas fa-eye"></i>';
        });
    });

    const signUpPassword = document.getElementById('signUpPassword');
    const strength = document.getElementById('passwordStrength');
    if (signUpPassword && strength) {
        signUpPassword.addEventListener('input', () => {
            const value = signUpPassword.value;
            let score = 0;
            if (value.length >= 10) score++;
            if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score++;
            if (/\d/.test(value)) score++;
            if (/[^A-Za-z0-9]/.test(value)) score++;
            strength.classList.remove('weak', 'medium', 'strong');
            if (!value) {
                strength.textContent = 'Use 10+ chars with letters, numbers & symbols.';
            } else if (score <= 1) {
                strength.classList.add('weak');
                strength.textContent = 'Weak password';
            } else if (score <= 3) {
                strength.classList.add('medium');
                strength.textContent = 'Moderate password';
            } else {
                strength.classList.add('strong');
                strength.textContent = 'Strong password';
            }
        });
    }

    if (signInForm) {
        signInForm.addEventListener("submit", () => {
            const status = document.getElementById('signInStatus');
            if (status) status.textContent = "Logging in...";

            const emailInput = document.getElementById("signInEmail");
            if (emailInput) {
                const email = emailInput.value;
                const namePart = email.split('@')[0] || 'User';
                const capitalizedName = namePart.charAt(0).toUpperCase() + namePart.slice(1);
                localStorage.setItem(profileKey, JSON.stringify({
                    fullName: capitalizedName,
                    email: email
                }));
            }
        });
    }

    if (signUpForm) {
        signUpForm.addEventListener("submit", () => {
            const nameInput = document.getElementById("signUpName");
            if (nameInput) {
                const name = nameInput.value;
                const email = document.getElementById("signUpEmail").value;
                localStorage.setItem(profileKey, JSON.stringify({
                    fullName: name,
                    email: email
                }));
            }
        });
    }
});
