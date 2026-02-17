const profileKey = "civicUserProfile";
const profileCompleteKey = "civicProfileComplete";

const appShell = document.getElementById("appShell");
const profileForm = document.getElementById("profileForm");
const detectLocation = document.getElementById("detectLocation");
const editProfileBtn = document.getElementById("editProfileBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");
const saveStatus = document.getElementById("saveStatus");

const fullName = document.getElementById("fullName");
const phoneNumber = document.getElementById("phoneNumber");
const email = document.getElementById("email");
const profession = document.getElementById("profession");
const address = document.getElementById("address");
const geoLocation = document.getElementById("geoLocation");
const otherInfo = document.getElementById("otherInfo");

const viewFullName = document.getElementById("viewFullName");
const viewPhoneNumber = document.getElementById("viewPhoneNumber");
const viewEmail = document.getElementById("viewEmail");
const viewProfession = document.getElementById("viewProfession");
const viewAddress = document.getElementById("viewAddress");
const viewGeoLocation = document.getElementById("viewGeoLocation");
const viewOtherInfo = document.getElementById("viewOtherInfo");

const MODES = {
  ENTRY: "entry-mode",
  DASHBOARD_VIEW: "dashboard-view-mode",
  DASHBOARD_EDIT: "dashboard-edit-mode"
};

const setMode = (mode) => {
  if (!appShell) return;
  appShell.classList.remove(MODES.ENTRY, MODES.DASHBOARD_VIEW, MODES.DASHBOARD_EDIT);
  appShell.classList.add(mode);
  
  // Toggle form and view visibility based on mode
  const detailsView = document.getElementById('detailsViewSection');
  if (mode === MODES.DASHBOARD_EDIT) {
    if (profileForm) profileForm.style.display = 'block';
    if (detailsView) detailsView.style.display = 'none';
  } else {
    if (profileForm) profileForm.style.display = 'none';
    if (detailsView) detailsView.style.display = 'block';
  }
};

const fallbackText = (value) => (value && value.trim() ? value.trim() : "Not provided");

// New Sync function to fill missing details from server
const syncWithServer = async () => {
    try {
        const response = await fetch('../DashboardServlet');
        if (!response.ok) return;
        const data = await response.json();
        
        const local = JSON.parse(localStorage.getItem(profileKey) || '{}');
        
        // Merge server data into local storage if local is empty/minimal
        if (!local.fullName || local.fullName === 'Member' || local.fullName === 'Restored User') {
            local.fullName = data.userName;
        }
        if (!local.email) local.email = data.email;
        if (!local.phone || local.phone === '+91 98765 43210') local.phone = data.phone;
        if (!local.address || local.address === '123 Civic Plaza, Central District') local.address = data.address;
        if (data.city && !local.city) local.city = data.city;

        localStorage.setItem(profileKey, JSON.stringify(local));
        fillForm(local);
        fillDetails(local);
        
        // Also update the Hero section name if it exists on the page
        const heroName = document.querySelector('.user-name');
        if (heroName) heroName.textContent = local.fullName;
        
        const avatarBig = document.querySelector('.profile-big-avatar');
        if (avatarBig && local.fullName) {
             const initials = local.fullName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
             avatarBig.textContent = initials;
        }

    } catch (e) {
        console.error("Sync failed:", e);
    }
};

const readFormData = () => ({
  fullName: fullName.value.trim(),
  phone: phoneNumber.value.trim(),
  email: email.value.trim(),
  profession: profession.value.trim(),
  address: address.value.trim(),
  geoLocation: geoLocation.value.trim(),
  otherInfo: otherInfo.value.trim()
});

const fillForm = (profile) => {
  fullName.value = profile.fullName || "";
  phoneNumber.value = profile.phone || "";
  email.value = profile.email || "";
  profession.value = profile.profession || "";
  address.value = profile.address || "";
  geoLocation.value = profile.geoLocation || "";
  otherInfo.value = profile.otherInfo || "";
};

const fillDetails = (profile) => {
  viewFullName.textContent = fallbackText(profile.fullName);
  viewPhoneNumber.textContent = fallbackText(profile.phone);
  viewEmail.textContent = fallbackText(profile.email);
  viewProfession.textContent = fallbackText(profile.profession);
  viewAddress.textContent = fallbackText(profile.address);
  viewGeoLocation.textContent = fallbackText(profile.geoLocation);
  viewOtherInfo.textContent = fallbackText(profile.otherInfo);
};

const setStatus = (message) => {
  if (!saveStatus) return;
  saveStatus.textContent = message;
};

let savedProfile = {};
const rawProfile = localStorage.getItem(profileKey);

if (rawProfile) {
  try {
    savedProfile = JSON.parse(rawProfile) || {};
  } catch (error) {
    console.error("Failed to parse saved profile:", error);
    savedProfile = {};
  }
}

fillForm(savedProfile);
fillDetails(savedProfile);
syncWithServer(); // Run async sync to fill missing details

const isProfileComplete = localStorage.getItem(profileCompleteKey) === "true";
if (isProfileComplete || (savedProfile.fullName && savedProfile.email)) {
  setMode(MODES.DASHBOARD_VIEW);
} else {
  setMode(MODES.DASHBOARD_VIEW); // Always show view mode, let user click Edit
}

detectLocation?.addEventListener("click", () => {
  if (!navigator.geolocation) {
    alert("Geolocation is not supported by your browser.");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const { latitude, longitude } = position.coords;
      geoLocation.value = `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
    },
    () => {
      alert("Unable to fetch location. Please allow location permission.");
    }
  );
});

profileForm?.addEventListener("submit", (event) => {
  event.preventDefault();

  const updatedProfile = readFormData();
  localStorage.setItem(profileKey, JSON.stringify(updatedProfile));
  localStorage.setItem(profileCompleteKey, "true");

  fillDetails(updatedProfile);
  setStatus("Profile saved successfully.");
  setMode(MODES.DASHBOARD_VIEW);
});

editProfileBtn?.addEventListener("click", () => {
  setStatus("");
  setMode(MODES.DASHBOARD_EDIT);
});

cancelEditBtn?.addEventListener("click", () => {
  const latest = localStorage.getItem(profileKey);

  if (latest) {
    try {
      const parsed = JSON.parse(latest) || {};
      fillForm(parsed);
      fillDetails(parsed);
    } catch (error) {
      console.error("Failed to parse profile while cancelling edit:", error);
    }
  }

  setStatus("");
  setMode(MODES.DASHBOARD_VIEW);
});
