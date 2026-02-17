// theme.js - Global Theme Management
document.addEventListener('DOMContentLoaded', () => {
    const themeToggles = document.querySelectorAll('#theme-toggle, .floating-theme-toggle, .theme-toggle-btn');
    const currentTheme = localStorage.getItem('theme') || 'light';
    
    // Apply initial theme
    document.documentElement.setAttribute('data-theme', currentTheme);
    themeToggles.forEach(btn => updateToggleBtn(btn, currentTheme));

    themeToggles.forEach(btn => {
        btn.addEventListener('click', () => {
            let theme = document.documentElement.getAttribute('data-theme');
            let newTheme = theme === 'dark' ? 'light' : 'dark';
            
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            themeToggles.forEach(b => updateToggleBtn(b, newTheme));
        });
    });

    function updateToggleBtn(btn, theme) {
        if (!btn) return;
        const icon = btn.querySelector('i');
        const text = btn.querySelector('span');
        
        if (theme === 'dark') {
            if (icon) {
                icon.className = 'fas fa-sun'; // Use fas for broader compatibility
            }
            if (text) text.innerText = 'Light Mode';
        } else {
            if (icon) {
                icon.className = 'fas fa-moon';
            }
            if (text) text.innerText = 'Dark Mode';
        }
    }
});
