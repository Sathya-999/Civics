// login.js - Simplified and Robust for Servlet Submission
const profileKey = "civicUserProfile";

document.addEventListener('DOMContentLoaded', () => {
    const signUpButton = document.getElementById("signUp");
    const signInButton = document.getElementById("signIn");
    const container = document.getElementById("container");
    const signInForm = document.getElementById("signInForm");
    const signUpForm = document.getElementById("signUpForm");

    // UI Switching Animation
    if (signUpButton && container) {
        signUpButton.addEventListener("click", () => {
            container.classList.add("right-panel-active");
            console.log("Switched to Sign Up");
        });
    }

    if (signInButton && container) {
        signInButton.addEventListener("click", () => {
            container.classList.remove("right-panel-active");
            console.log("Switched to Sign In");
        });
    }

    // Login Form Preparation
    if (signInForm) {
        signInForm.addEventListener("submit", (e) => {
            // Remove error message if exists
            const status = document.getElementById('signInStatus');
            if(status) status.textContent = "Logging in...";
            
            const emailInput = document.getElementById("signInEmail");
            if (emailInput) {
                const email = emailInput.value;
                const namePart = email.split('@')[0];
                const capitalizedName = namePart.charAt(0).toUpperCase() + namePart.slice(1);
                localStorage.setItem(profileKey, JSON.stringify({
                    fullName: capitalizedName,
                    email: email
                }));
            }
            console.log("Submitting Login...");
            // Browser handles the POST to /LoginServlet
        });
    }

    // Register Form Preparation
    if (signUpForm) {
        signUpForm.addEventListener("submit", (e) => {
            const nameInput = document.getElementById("signUpName");
            if (nameInput) {
                const name = nameInput.value;
                const email = document.getElementById("signUpEmail").value;
                localStorage.setItem(profileKey, JSON.stringify({
                    fullName: name,
                    email: email
                }));
            }
            console.log("Submitting Registration...");
            // Browser handles the POST to /RegisterServlet
        });
    }

    // Google Sign-In is handled by Google Identity Services in login.html
    // No need for manual button handling here
});
