// portal-core.js - Shared logic for all portal pages
document.addEventListener('DOMContentLoaded', () => {
    initToasts();
    initUserProfile();
    checkFlashMessages();
    highlightActiveMenu();
    initChatbot(); // Initialize Global AI Agent
});

function initChatbot() {
    // Don't add floating chatbot widget on the dedicated chatbot page (duplicate IDs conflict)
    if (window.location.pathname.includes('chatbot.html')) return;

    const existingBtn = document.getElementById('chatToggle');
    if (existingBtn) return;

    // Create Floating Toggle Button
    const btn = document.createElement('button');
    btn.id = 'chatToggle';
    btn.className = 'chat-toggle';
    btn.innerHTML = '<i class="fas fa-comment-dots"></i>';
    btn.style.cssText = "position:fixed; bottom:30px; right:30px; width:60px; height:60px; border-radius:50%; background:#007bff; color:white; border:none; box-shadow:0 10px 25px rgba(0,123,255,0.3); cursor:pointer; z-index:10001; font-size:1.5rem; display:flex; align-items:center; justify-content:center; transition:all 0.3s ease;";
    btn.onclick = window.toggleChat;
    
    btn.onmouseover = () => btn.style.transform = "scale(1.1) rotate(5deg)";
    btn.onmouseout = () => btn.style.transform = "scale(1) rotate(0)";

    // Create Chat Window
    const win = document.createElement('div');
    win.id = 'chatWindow';
    win.className = 'chat-window';
    win.style.display = 'none';
    win.innerHTML = `
        <div class="chat-header">
            <span><i class="fas fa-robot"></i> CAI - Civic Assistant</span>
            <i class="fas fa-times" onclick="toggleChat()" style="cursor:pointer"></i>
        </div>
        <div class="chat-body" id="chatBody">
            <div class="message bot-msg">Hello! I'm CAI. How can I help you today?</div>
            <div id="typing" class="message bot-msg" style="display:none; font-style:italic; opacity:0.7">CAI is thinking...</div>
        </div>
        <div class="chat-footer" style="padding:15px; border-top:1px solid var(--border-light); background:var(--card-bg); display:flex; gap:10px;">
            <input type="text" id="chatInput" placeholder="Type your issue..." style="flex:1; padding:8px 12px; border-radius:20px; border:1px solid var(--border-light); background:var(--bg-main); color:var(--text-main);">
            <button onclick="sendMessage()" style="background:#007bff; color:white; border:none; width:35px; height:35px; border-radius:50%; cursor:pointer;"><i class="fas fa-paper-plane"></i></button>
        </div>
    `;

    document.body.appendChild(btn);
    document.body.appendChild(win);

    // Initialize enter key listener
    setTimeout(() => {
        const input = document.getElementById('chatInput');
        if(input) {
            input.onkeypress = (e) => { if (e.key === 'Enter') sendMessage(); };
        }
    }, 500);
}

window.toggleChat = function() {
    const chatWindow = document.getElementById('chatWindow');
    if (chatWindow) {
        chatWindow.style.display = chatWindow.style.display === 'flex' ? 'none' : 'flex';
    }
};

window.sendMessage = async function() {
    const chatInput = document.getElementById('chatInput');
    const chatBody = document.getElementById('chatBody');
    const typing = document.getElementById('typing');
    
    const text = chatInput.value.trim();
    if (!text) return;

    addMessage(text, 'user-msg');
    chatInput.value = '';
    if (typing) typing.style.display = 'block';

    try {
        const r = await fetch('../ChatbotServlet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text })
        });
        if (!r.ok) throw new Error();
        const data = await r.json();
        displayAiResponse(data);
    } catch {
        setTimeout(() => simulateAI(text), 1000);
    }
};

function simulateAI(text) {
    const typing = document.getElementById('typing');
    if (typing) typing.style.display = 'none';
    
    let r = { type: 'INFO', reply: "I'm CAI. I can help you report civic issues like potholes or garbage." };
    const lt = text.toLowerCase();
    
    if (lt.includes('pothole') || lt.includes('road') || lt.includes('waste') || lt.includes('garbage') || lt.includes('water') || lt.includes('street light')) {
        let cat = 'General';
        if (lt.includes('road') || lt.includes('pothole')) cat = 'Roads';
        if (lt.includes('water')) cat = 'Water Supply';
        if (lt.includes('waste') || lt.includes('garbage')) cat = 'Sanitation';
        if (lt.includes('light')) cat = 'Electricity';
        r = { type: 'COMPLAINT', category: cat, desc: text, reply: `I've identified a potential ${cat} issue: "${text}". Would you like to file a formal complaint?` };
    }
    displayAiResponse(r);
}

function displayAiResponse(r) {
    const chatBody = document.getElementById('chatBody');
    const typing = document.getElementById('typing');
    if (typing) typing.style.display = 'none';

    if (r.type === 'COMPLAINT') {
        addMessage(r.reply, 'bot-msg');
        const btn = document.createElement('a');
        btn.className = 'action-btn';
        btn.style.cssText = "background:#2ecc71; color:white; border:none; padding:8px 12px; border-radius:6px; margin-top:8px; cursor:pointer; font-size:0.8rem; display:inline-block; text-decoration:none;";
        btn.href = `raise-complaint.html?cat=${encodeURIComponent(r.category || 'General')}&desc=${encodeURIComponent(r.desc || 'Via CAI')}`;
        btn.innerHTML = `<i class="fas fa-file-signature"></i> Raise Formal Complaint`;
        chatBody.appendChild(btn);
    } else {
        addMessage(r.reply || 'I am here to assist.', 'bot-msg');
    }
    chatBody.scrollTop = chatBody.scrollHeight;
}

function addMessage(txt, cls) {
    const chatBody = document.getElementById('chatBody');
    const d = document.createElement('div');
    d.className = `message ${cls}`;
    d.innerText = txt;
    chatBody.appendChild(d);
    chatBody.scrollTop = chatBody.scrollHeight;
}

function initToasts() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
}

window.showToast = function(title, message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let iconClass = 'fa-solid fa-circle-info';
    if (type === 'success') iconClass = 'fa-solid fa-circle-check';
    if (type === 'error') iconClass = 'fa-solid fa-circle-exclamation';

    toast.innerHTML = `
        <div class="toast-icon">
            <i class="${iconClass}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <div class="toast-close" onclick="this.parentElement.classList.remove('show'); setTimeout(() => this.parentElement.remove(), 500);">
            <i class="fa-solid fa-xmark"></i>
        </div>
    `;

    container.appendChild(toast);
    
    // Trigger animation
    setTimeout(() => toast.classList.add('show'), 100);

    // Auto remove
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 500);
    }, 5000);
};

function checkFlashMessages() {
    const cookies = document.cookie.split('; ');
    const flashMsg = cookies.find(c => c.startsWith('flash_msg='));
    const flashError = cookies.find(c => c.startsWith('flash_error='));

    if (flashMsg) {
        const msg = decodeURIComponent(flashMsg.split('=')[1]);
        showToast('Success', msg, 'success');
        // Clear cookie
        document.cookie = "flash_msg=; Path=/; Max-Age=0";
    }

    if (flashError) {
        const msg = decodeURIComponent(flashError.split('=')[1]);
        showToast('Error', msg, 'error');
        // Clear cookie
        document.cookie = "flash_error=; Path=/; Max-Age=0";
    }
}

function initUserProfile() {
    // Try to get user from localStorage or simple placeholder
    const profile = JSON.parse(localStorage.getItem('civicUserProfile') || '{"fullName": "Member", "email": "User"}');
    
    // Update all elements with class 'user-name'
    const nameEls = document.querySelectorAll('.user-name');
    nameEls.forEach(el => el.textContent = profile.fullName || "Member");

    // Update welcome message if it exists on the page
    const welcomeEl = document.getElementById('welcomeUser');
    if (welcomeEl) {
        welcomeEl.textContent = `Welcome, ${profile.fullName || 'Citizen'}!`;
    }

    const avatarEls = document.querySelectorAll('.user-avatar, .user-avatar-small');
    avatarEls.forEach(el => {
        if (el.tagName === 'IMG' && profile.fullName) {
             el.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(profile.fullName)}&background=007bff&color=fff`;
        }
    });
}

function highlightActiveMenu() {
    const currentPath = window.location.pathname;
    const menuItems = document.querySelectorAll('.menu-item');
    
    menuItems.forEach(item => {
        // High-end: Wrap raw text nodes in <span> for better CSS control (collapsed sidebar)
        const textNode = Array.from(item.childNodes).find(n => n.nodeType === 3 && n.textContent.trim().length > 0);
        if (textNode) {
            const span = document.createElement('span');
            span.textContent = textNode.textContent;
            item.replaceChild(span, textNode);
        }

        const href = item.getAttribute('href');
        if (href && currentPath.includes(href)) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
}

function logout() {
    localStorage.removeItem('civicUserProfile');
    window.location.href = '../LogoutServlet';
}

window.showHowToUse = function() {
    const modalId = 'howToUseModal';
    let modal = document.getElementById(modalId);
    
    if (!modal) {
        modal = document.createElement('div');
        modal.id = modalId;
        modal.style.cssText = "position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:flex; align-items:center; justify-content:center; z-index:20000; backdrop-filter:blur(5px);";
        modal.innerHTML = `
            <div style="background:var(--card-bg); width:90%; max-width:600px; padding:30px; border-radius:20px; box-shadow:0 20px 50px rgba(0,0,0,0.3); color:var(--text-main); position:relative; animation: slideUp 0.4s ease;">
                <button onclick="this.closest('#${modalId}').remove()" style="position:absolute; top:20px; right:20px; background:none; border:none; font-size:1.5rem; color:var(--text-main); cursor:pointer;"><i class="fas fa-times"></i></button>
                <h2 style="margin-bottom:20px; color:#007bff;"><i class="fas fa-book-open"></i> How to use Civic Connect</h2>
                
                <div style="display:grid; gap:20px;">
                    <div style="display:flex; gap:15px; align-items:start;">
                        <div style="background:#e7f3ff; color:#007bff; width:40px; height:40px; border-radius:10px; display:flex; align-items:center; justify-content:center; flex-shrink:0;"><i class="fas fa-plus"></i></div>
                        <div>
                            <h4 style="margin-bottom:5px;">Report Issues</h4>
                            <p style="font-size:0.9rem; opacity:0.8;">Go to "Raise Complaint" and use the AI or form to report potholes, garbage, or street light issues.</p>
                        </div>
                    </div>
                    <div style="display:flex; gap:15px; align-items:start;">
                        <div style="background:#fff4e6; color:#fd7e14; width:40px; height:40px; border-radius:10px; display:flex; align-items:center; justify-content:center; flex-shrink:0;"><i class="fas fa-bullseye"></i></div>
                        <div>
                            <h4 style="margin-bottom:5px;">Track Status</h4>
                            <p style="font-size:0.9rem; opacity:0.8;">Check the progress of your complaints in "Track Complaint". See when they are verified and resolved.</p>
                        </div>
                    </div>
                    <div style="display:flex; gap:15px; align-items:start;">
                        <div style="background:#ebfbee; color:#40c057; width:40px; height:40px; border-radius:10px; display:flex; align-items:center; justify-content:center; flex-shrink:0;"><i class="fas fa-coins"></i></div>
                        <div>
                            <h4 style="margin-bottom:5px;">Earn Civic Points</h4>
                            <p style="font-size:0.9rem; opacity:0.8;">Get points for every resolved issue. Use these points to improve your civic rank in the city.</p>
                        </div>
                    </div>
                </div>

                <button onclick="this.closest('#${modalId}').remove()" style="width:100%; margin-top:30px; padding:12px; background:#007bff; color:white; border:none; border-radius:10px; font-weight:600; cursor:pointer;">Got it!</button>
            </div>
        `;
        document.body.appendChild(modal);
    } else {
        modal.style.display = 'flex';
    }
};
