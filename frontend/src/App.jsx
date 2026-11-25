import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import "leaflet/dist/leaflet.css";
import {
  ArrowDown, ArrowLeft, ArrowRight, Bell, Bot, CalendarDays, Check, CheckCircle2,
  BriefcaseBusiness, ChevronDown, ChevronRight, CircleHelp, Clock3, Flower2, Heart, LayoutDashboard,
  ListChecks, LogOut, Mail, MapPin, Menu, MessageCircle, Moon, Palette, Pencil, Type,
  Globe, Paperclip, Phone, RotateCcw, Search, Send, ShieldCheck, SlidersHorizontal, Sparkles, Star, Sun,
  Trash2, Users, Utensils, X,
} from "lucide-react";
import {
  adminApi, assistantApi, authApi, bookingsApi, categoriesApi, checklistApi, collaborationsApi, countriesApi, guestApi, messagesApi,
  notificationsApi, supplierServicesApi, usersApi,
  paymentsApi, resolveMediaUrl, reviewsApi, suppliersApi, websocketUrl,
} from "./api.js";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import { Client } from "@stomp/stompjs";
import { getCountries, getCountryCallingCode, parsePhoneNumberFromString } from "libphonenumber-js";

const stripePromise = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY
  ? loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY)
  : null;

const pricingTypeLabels = {
  PERSON: "pricingPerPerson",
  TABLE: "pricingPerTable",
  WEDDING: "pricingAllWedding",
};
const activeServiceBookingStatuses = new Set(["REQUESTED", "QUOTED", "ACCEPTED", "CONFIRMED", "NEW_REQUEST", "UNDER_REVIEW", "SENT_TO_COUPLE", "ACCEPTED_BY_COUPLE", "WAITING_PAYMENT", "ACTIVE"]);

function localDateString(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function BrandContent() {
  return <><span className="brand-mark"><img src="/favicon.png" alt="" /></span>
    <span><span className="brand-period">.</span>wedMasterAI</span></>;
}

function daysUntilDate(dateString) {
  const [year, month, day] = dateString.split("-").map(Number);
  const today = new Date();
  const weddingDay = Date.UTC(year, month - 1, day);
  const todayUtc = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((weddingDay - todayUtc) / 86400000);
}

const categories = [
  { slug: "venues", name: "Venues", icon: "⌂", count: 128, tone: "rose" },
  { slug: "catering", name: "Catering", icon: "♨", count: 86, tone: "sand" },
  { slug: "flowers", name: "Flowers", icon: "✿", count: 94, tone: "sage" },
  { slug: "photography", name: "Photography", icon: "◉", count: 72, tone: "lilac" },
  { slug: "dresses", name: "Dresses", icon: "♧", count: 58, tone: "peach" },
  { slug: "hair", name: "Hair", icon: "✧", count: 36, tone: "blue" },
  { slug: "makeup", name: "Makeup", icon: "✧", count: 27, tone: "blue" },
];

const suppliers = [
  { id: 1, name: "Casa Lume Estate", category: "Venues", place: "Tuscany, Italy", rating: "4.98", reviews: 124, price: "€4,200", image: "photo-1519225421980-715cb0215aed", tag: "A little magic", verified: true },
  { id: 2, name: "The Olive & Thyme", category: "Catering", place: "Provence, France", rating: "4.96", reviews: 89, price: "€115", priceSuffix: "pricePerGuest", image: "photo-1511795409834-ef04bbd61622", tag: "Guest favourite", verified: true },
  { id: 3, name: "Petal & Poetry", category: "Flowers", place: "Athens, Greece", rating: "5.0", reviews: 67, price: "€850", pricePrefix: true, image: "photo-1490750967868-88aa4486c946", tag: "Date availability", verified: true },
  { id: 4, name: "Sofia Moreau Studio", category: "Photography", place: "Nice, France", rating: "4.99", reviews: 102, price: "€2,100", pricePrefix: true, image: "photo-1537633552985-df8429e8048b", tag: "A little magic", verified: true },
  { id: 5, name: "Atelier Amara", category: "Dresses", place: "Paris, France", rating: "4.95", reviews: 56, price: "€1,800", pricePrefix: true, image: "photo-1594552072238-b8a33785b261", tag: "Guest favourite", verified: true },
  { id: 6, name: "Sunday Glow", category: "Hair & makeup", place: "Santorini, Greece", rating: "4.97", reviews: 76, price: "€420", pricePrefix: true, image: "photo-1522335789203-aabd1fc54bc9", tag: "Date availability", verified: true },
];

const customerNavigation = [
  { id: "home", icon: LayoutDashboard },
  { id: "discover", icon: Search },
  { id: "checklist", icon: ListChecks },
  { id: "guests", icon: Users },
  { id: "invitations", icon: Mail },
  { id: "bookings", icon: CalendarDays },
  { id: "messages", icon: MessageCircle },
  { id: "saved", icon: Heart },
  { id: "collaborations", icon: Users },
];

const workspacePages = {
  CUSTOMER: ["home", "discover", "checklist", "guests", "invitations", "bookings", "messages", "saved", "collaborations"],
  SUPPLIER: ["supplierDashboard", "discover", "bookings", "messages", "collaborations"],
  ADMIN: ["admin"],
};

const imageUrl = (id, width = 720) => id?.startsWith("/uploads/")
  ? resolveMediaUrl(id)
  : id?.startsWith("http")
    ? id
    : `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=85`;
const supportedPhoneCountries = getCountries();

function formatSupplierPrice(t, supplier) {
  if (supplier.pricePrefix) return t("priceFrom", { price: supplier.price });
  if (supplier.priceSuffix) return t(supplier.priceSuffix, { price: supplier.price });
  return supplier.price;
}

function supplierCategoryLabel(t, category) {
  const categoryKeys = {
    Venues: "categoryVenues",
    Catering: "categoryCatering",
    Flowers: "categoryFlowers",
    Photography: "categoryPhotography",
    Dresses: "categoryDresses",
    Hair: "categoryHair",
    Makeup: "categoryMakeup",
    "Hair & makeup": "categoryHairMakeup",
  };
  return categoryKeys[category] ? t(categoryKeys[category]) : category;
}

function workspacePathFor(account) {
  if (account?.role === "ADMIN") return "/admin";
  if (account?.role === "SUPPLIER") return "/supplierDashboard";
  if (account?.role === "CUSTOMER") return "/home";
  return "/";
}

function readSavedSuppliers(accountId) {
  try {
    return JSON.parse(localStorage.getItem(`wedora-saved-${accountId}`) || "[]");
  } catch {
    return [];
  }
}

function readSavedInvitation(accountId) {
  try {
    const saved = JSON.parse(localStorage.getItem(`wedora-invitation-${accountId}`) || "null");
    const validEventDate = !saved?.eventDate
      || (/^\d{4}-\d{2}-\d{2}$/.test(saved.eventDate)
        && new Date(`${saved.eventDate}T00:00:00Z`).toISOString().slice(0, 10) === saved.eventDate);
    return saved && typeof saved === "object"
      && ["garden", "editorial", "classic"].includes(saved.templateId)
      && typeof saved.coupleNames === "string"
      && typeof saved.eventDate === "string"
      && validEventDate
      && typeof saved.venue === "string"
      && typeof saved.message === "string"
      ? saved
      : null;
  } catch {
    return null;
  }
}

function drawInvitationText(context, text, x, y, maxWidth, lineHeight, maxLines = Infinity) {
  const words = String(text).split(/\s+/);
  const lines = [];
  let line = "";
  words.forEach((word) => {
    const candidate = line ? `${line} ${word}` : word;
    if (line && context.measureText(candidate).width > maxWidth) {
      lines.push(line);
      line = word;
    } else {
      line = candidate;
    }
  });
  if (line) lines.push(line);
  const visibleLines = lines.slice(0, maxLines);
  if (lines.length > maxLines) visibleLines[maxLines - 1] = `${visibleLines[maxLines - 1].replace(/[.…]+$/, "")}…`;
  visibleLines.forEach((lineText, index) => context.fillText(lineText, x, y + index * lineHeight, maxWidth));
  return visibleLines.length * lineHeight;
}

function createInvitationImage(templateId, coupleNames, message, weddingDate, venue, eyebrow, language) {
  const canvas = document.createElement("canvas");
  canvas.width = 1080;
  canvas.height = 1350;
  const context = canvas.getContext("2d");
  if (!context) throw new Error("Invitation canvas is unavailable");

  const palettes = {
    garden: { background: "#fbf7f0", accent: "#788b77", tint: "#c9a0a1" },
    editorial: { background: "#f7f2eb", accent: "#775c4b", tint: "#b99068" },
    classic: { background: "#f6f1e9", accent: "#875d67", tint: "#9a6c74" },
  };
  const palette = palettes[templateId] || palettes.garden;
  context.fillStyle = palette.background;
  context.fillRect(0, 0, canvas.width, canvas.height);

  const glow = context.createRadialGradient(1080, 0, 10, 1080, 0, 620);
  glow.addColorStop(0, `${palette.tint}35`);
  glow.addColorStop(1, `${palette.tint}00`);
  context.fillStyle = glow;
  context.fillRect(0, 0, canvas.width, canvas.height);
  const lowerGlow = context.createRadialGradient(0, 1350, 0, 0, 1350, 580);
  lowerGlow.addColorStop(0, `${palette.accent}28`);
  lowerGlow.addColorStop(1, `${palette.accent}00`);
  context.fillStyle = lowerGlow;
  context.fillRect(0, 0, canvas.width, canvas.height);

  context.strokeStyle = `${palette.accent}60`;
  context.lineWidth = templateId === "classic" ? 3 : 2;
  context.strokeRect(44, 44, canvas.width - 88, canvas.height - 88);
  if (templateId === "classic") {
    context.strokeRect(56, 56, canvas.width - 112, canvas.height - 112);
  }

  const centerX = canvas.width / 2;
  context.fillStyle = palette.accent;
  context.globalAlpha = 0.8;
  for (let petal = 0; petal < 8; petal += 1) {
    const angle = (Math.PI * 2 * petal) / 8;
    context.beginPath();
    context.ellipse(
      centerX + Math.cos(angle) * 23,
      225 + Math.sin(angle) * 23,
      10,
      19,
      angle,
      0,
      Math.PI * 2,
    );
    context.fill();
  }
  context.globalAlpha = 1;

  context.textAlign = "center";
  context.textBaseline = "alphabetic";
  context.direction = language === "ar" ? "rtl" : "ltr";
  context.fillStyle = palette.accent;
  context.font = "600 21px 'DM Sans', Arial, sans-serif";
  context.fillText(eyebrow.toLocaleUpperCase(language), centerX, 320, 850);

  context.font = templateId === "editorial"
    ? "500 76px 'DM Sans', Arial, sans-serif"
    : templateId === "classic"
      ? "italic 500 78px Georgia, serif"
      : "500 82px Georgia, serif";
  const nameHeight = drawInvitationText(context, coupleNames, centerX, 465, 820, 96, 2);

  context.fillStyle = palette.accent;
  context.font = "400 30px 'DM Sans', Arial, sans-serif";
  const messageHeight = drawInvitationText(context, message, centerX, 520 + nameHeight, 720, 45, 6);

  let detailY = 570 + nameHeight + messageHeight;
  context.font = "600 26px 'DM Sans', Arial, sans-serif";
  if (weddingDate) {
    context.fillText(weddingDate, centerX, detailY, 800);
    detailY += 64;
  }
  if (venue) {
    context.font = "400 25px 'DM Sans', Arial, sans-serif";
    drawInvitationText(context, venue, centerX, detailY, 760, 36, 2);
  }

  context.font = "400 27px Georgia, serif";
  context.fillText("✦", centerX, 1120);
  const dataUrl = canvas.toDataURL("image/png");
  const imageData = atob(dataUrl.slice(dataUrl.indexOf(",") + 1));
  const imageBytes = Uint8Array.from(imageData, (character) => character.charCodeAt(0));
  return new Blob([imageBytes], { type: "image/png" });
}

function readAppearance() {
  try {
    const saved = JSON.parse(localStorage.getItem("wedora-appearance") || "{}");
    return {
      palette: ["rose_champagne", "sage_ivory", "terracotta_cream", "plum_gold"].includes(saved.palette)
        ? saved.palette : "rose_champagne",
      typography: ["editorial", "modern", "romantic"].includes(saved.typography)
        ? saved.typography : "editorial",
      density: ["comfortable", "compact"].includes(saved.density) ? saved.density : "comfortable",
      darkMode: saved.darkMode === true,
    };
  } catch {
    return { palette: "rose_champagne", typography: "editorial", density: "comfortable", darkMode: false };
  }
}

function AppearanceControl({ appearance, onChange }) {
  const { t } = useTranslation();
  const paletteOptions = [
    { id: "rose_champagne", name: t("paletteRose"), color: "#a85669", accent: "#bd9661" },
    { id: "sage_ivory", name: t("paletteSage"), color: "#71866f", accent: "#c5a36d" },
    { id: "terracotta_cream", name: t("paletteTerracotta"), color: "#b9684f", accent: "#d0a16c" },
    { id: "plum_gold", name: t("palettePlum"), color: "#70415e", accent: "#c4a15f" },
  ];
  return <details className="appearance-control">
    <summary aria-label={t("appearanceSettings")} title={t("appearanceSettings")}><Palette size={17} /><span>{t("appearance")}</span></summary>
    <div className="appearance-popover">
      <div className="appearance-popover-heading">
        <div><strong>{t("appearanceSettings")}</strong><p>{t("appearanceIntro")}</p></div>
        <Type size={17} aria-hidden="true" />
      </div>
      <fieldset className="appearance-palette-options">
        <legend>{t("colorPalette")}</legend>
        {paletteOptions.map((option) => <button type="button" key={option.id}
          className={`appearance-palette-option${appearance.palette === option.id ? " selected" : ""}`}
          aria-pressed={appearance.palette === option.id}
          onClick={() => onChange((current) => ({ ...current, palette: option.id }))}>
          <span className="appearance-swatch" aria-hidden="true"><i style={{ backgroundColor: option.color }} /><i style={{ backgroundColor: option.accent }} /></span>
          <span>{option.name}</span>
        </button>)}
      </fieldset>
      <label className="appearance-select-label">{t("fontStyle")}
        <select value={appearance.typography} onChange={(event) => onChange((current) => ({ ...current, typography: event.target.value }))}>
          <option value="editorial">{t("fontEditorial")}</option>
          <option value="modern">{t("fontModern")}</option>
          <option value="romantic">{t("fontRomantic")}</option>
        </select>
      </label>
      <fieldset className="appearance-density-options">
        <legend>{t("displayDensity")}</legend>
        <button type="button" className={appearance.density === "comfortable" ? "selected" : ""}
          aria-pressed={appearance.density === "comfortable"}
          onClick={() => onChange((current) => ({ ...current, density: "comfortable" }))}>{t("comfortable")}</button>
        <button type="button" className={appearance.density === "compact" ? "selected" : ""}
          aria-pressed={appearance.density === "compact"}
          onClick={() => onChange((current) => ({ ...current, density: "compact" }))}>{t("compact")}</button>
      </fieldset>
    </div>
  </details>;
}

function App() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const supplierRoute = location.pathname.match(/^\/supplier\/([^/]+)$/);
  const supplierSlug = supplierRoute ? decodeURIComponent(supplierRoute[1]) : null;
  const isLandingPath = location.pathname === "/";
  const [account, setAccount] = useState(() => {
    try { return JSON.parse(localStorage.getItem("wedora-user") || "null"); }
    catch { return null; }
  });
  const [supplierCollaborationEligible, setSupplierCollaborationEligible] = useState(false);
  const [supplierCollaborationAccessChecked, setSupplierCollaborationAccessChecked] = useState(false);
  const isPublicLanding = isLandingPath && !account;
  const activePage = supplierRoute
    ? (location.state?.returnPage || "discover")
    : isLandingPath && account?.role === "SUPPLIER" ? "supplierDashboard"
      : isLandingPath && account?.role === "ADMIN" ? "admin"
        : location.pathname.replace(/^\//, "").split("/")[0] || "home";
  function setActivePage(page) {
    navigate(page === "home" ? workspacePathFor(account) : `/${page}`);
  }
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [query, setQuery] = useState("");
  const [saved, setSaved] = useState(() => account ? readSavedSuppliers(account.id) : []);
  const [tasks, setTasks] = useState([]);
  const [selectedCollaborations, setSelectedCollaborations] = useState([]);
  const [appearance, setAppearance] = useState(readAppearance);
  const { palette, typography, density, darkMode } = appearance;
  const [language, setLanguage] = useState(i18n.language);
  const [selectedSupplier, setSelectedSupplier] = useState(null);
  const [bookingSupplier, setBookingSupplier] = useState(null);
  const [notice, setNotice] = useState("");
  const [mobileNav, setMobileNav] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState("login");
  const [liveSuppliers, setLiveSuppliers] = useState(null);
  const [directoryLoaded, setDirectoryLoaded] = useState(false);
  const [directoryError, setDirectoryError] = useState(false);
  const [directoryCategories, setDirectoryCategories] = useState(categories);
  const [countryOptions, setCountryOptions] = useState([]);
  const [countryLoadStatus, setCountryLoadStatus] = useState("loading");
  const [countryLoadAttempt, setCountryLoadAttempt] = useState(0);
  const [apiBookings, setApiBookings] = useState(null);
  const [bookingsLoadError, setBookingsLoadError] = useState(false);
  const [checklistLoaded, setChecklistLoaded] = useState(false);
  const [checklistLoadError, setChecklistLoadError] = useState(false);
  const [paymentRecords, setPaymentRecords] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [focusedMessageBookingId, setFocusedMessageBookingId] = useState(null);
  const seenNotificationIds = useRef(new Set());
  const liveNotifications = useRef([]);
  const [paymentBookingId, setPaymentBookingId] = useState(null);
  const [quoteBooking, setQuoteBooking] = useState(null);
  const [bookingDetails, setBookingDetails] = useState(null);
  const [checklistModalOpen, setChecklistModalOpen] = useState(false);
  const [checklistEditingTask, setChecklistEditingTask] = useState(null);
  const [reviewBooking, setReviewBooking] = useState(null);
  const [reviewedBookingIds, setReviewedBookingIds] = useState([]);
  const catalog = directoryLoaded ? (liveSuppliers || []) : suppliers;
  const returnPath = supplierRoute ? (location.state?.returnPath || "/discover") : null;

  useEffect(() => {
    setSaved(account?.role === "CUSTOMER" ? readSavedSuppliers(account.id) : []);
    setTasks([]);
    setSelectedCollaborations([]);
    setApiBookings(null);
    setBookingsLoadError(false);
    setChecklistLoaded(false);
    setChecklistLoadError(false);
    setPaymentRecords([]);
  }, [account]);
  useEffect(() => {
    let active = true;
    if (account?.role !== "SUPPLIER") {
      setSupplierCollaborationEligible(false);
      setSupplierCollaborationAccessChecked(true);
      return () => { active = false; };
    }
    setSupplierCollaborationEligible(false);
    setSupplierCollaborationAccessChecked(false);
    suppliersApi.owned().then(({ data }) => {
      if (active) {
        setSupplierCollaborationEligible(data.some((profile) => profile.approved && profile.active !== false));
        setSupplierCollaborationAccessChecked(true);
      }
    }).catch((error) => {
      if (active) {
        setSupplierCollaborationAccessChecked(true);
        setNotice(error.response?.data?.detail || t("errorLoadBusinessProfile"));
      }
    });
    return () => { active = false; };
  }, [account?.id, account?.role, t]);
  useEffect(() => {
    localStorage.setItem("wedora-appearance", JSON.stringify(appearance));
  }, [appearance]);
  useEffect(() => {
    if (isLandingPath && account) {
      navigate(workspacePathFor(account));
      return;
    }
    if (supplierRoute || isPublicLanding) return;
    if (!account) {
      if (activePage !== "discover") navigate("/");
      return;
    }
    if (account.role === "SUPPLIER" && activePage === "collaborations"
        && !supplierCollaborationAccessChecked) return;
    const rolePages = workspacePages[account.role] || [];
    if (!rolePages.includes(activePage)
        || (account.role === "SUPPLIER" && activePage === "collaborations" && !supplierCollaborationEligible)) {
      navigate(workspacePathFor(account));
    }
  }, [activePage, account, isLandingPath, isPublicLanding, navigate, supplierRoute,
    supplierCollaborationAccessChecked, supplierCollaborationEligible]);
  useEffect(() => {
    suppliersApi.list().then(({ data }) => {
      if (data.length) {
        setLiveSuppliers(data.map((supplier) => ({
          ...supplier,
          name: supplier.businessName,
          place: supplier.location,
          category: supplier.categories?.map((category) => category.name).filter(Boolean).join(", ") || supplier.categoryName,
          rating: Number(supplier.rating || 0).toFixed(2),
          reviews: supplier.reviewCount || 0,
          price: supplier.startingPrice ? `${supplier.currency} ${supplier.startingPrice}` : t("askForQuote"),
          image: supplier.mainImageUrl || "photo-1519225421980-715cb0215aed",
          tag: "Verified supplier",
          verified: supplier.approved,
        })));
      }
      setDirectoryError(false);
      setDirectoryLoaded(true);
    }).catch(() => {
      setDirectoryError(true);
      setDirectoryLoaded(true);
      setNotice(t("directoryOffline"));
    });
    const refreshCategories = () => categoriesApi.list().then(({ data }) => {
      if (data.length) {
        const tones = ["rose", "sand", "sage", "lilac", "peach", "blue"];
        const symbols = ["⌂", "♨", "✿", "◉", "♧", "✧"];
        setDirectoryCategories(data.map((category, index) => ({
          ...category,
          count: 0,
          tone: tones[index % tones.length],
          icon: symbols[index % symbols.length],
        })));
      }
    }).catch((error) => setNotice(error.response?.data?.detail || t("errorLoadCategories")));
    refreshCategories();
    window.addEventListener("wedora:categories-updated", refreshCategories);
    return () => window.removeEventListener("wedora:categories-updated", refreshCategories);
  }, [language, t]);
  useEffect(() => {
    let active = true;
    setCountryLoadStatus("loading");
    countriesApi.list().then(({ data }) => {
      if (!Array.isArray(data) || data.length === 0
          || data.some((country) => !country.code || !country.name)) {
        throw new Error("The countries API returned an invalid country list");
      }
      if (active) {
        setCountryOptions(data);
        setCountryLoadStatus("loaded");
      }
    }).catch((error) => {
      if (!active) return;
      setCountryOptions([]);
      setCountryLoadStatus("error");
      setNotice(error.response?.data?.detail || t("errorLoadCountries"));
    });
    return () => { active = false; };
  }, [countryLoadAttempt, t]);
  function retryCountryLoad() {
    setCountryLoadAttempt((attempt) => attempt + 1);
  }
  useEffect(() => {
    if (!supplierRoute) {
      setSelectedSupplier(null);
      return;
    }
    const slug = supplierSlug;
    const match = catalog.find((supplier) => supplier.slug === slug || String(supplier.id) === slug);
    if (match) {
      if (account?.role === "CUSTOMER" && directoryLoaded
          && !(account.countryCodes || []).some((code) => (match.countryCodes || []).includes(code))) {
        setSelectedSupplier(null);
        navigate("/discover", { replace: true });
        setNotice(t("supplierNotInSelectedCountries"));
        return;
      }
      setSelectedSupplier(match);
      if (directoryLoaded) {
        suppliersApi.get(match.id).then(({ data }) => setSelectedSupplier({
          ...match,
          ...data,
          name: data.businessName,
          place: data.location,
          category: data.categories?.map((category) => category.name).filter(Boolean).join(", ") || data.categoryName,
          rating: Number(data.rating || 0).toFixed(2),
          reviews: data.reviewCount || 0,
          price: data.startingPrice ? `${data.currency} ${data.startingPrice}` : t("askForQuote"),
          image: data.mainImageUrl || match.image,
        })).catch((error) => setNotice(error.response?.data?.detail || "We couldn't load this supplier profile."));
      }
      return;
    }
    const request = /^\d+$/.test(slug) ? suppliersApi.get(slug) : suppliersApi.getBySlug(slug);
    request.then(({ data }) => {
      const supplier = {
        ...data,
        name: data.businessName,
        place: data.location,
        category: data.categories?.map((category) => category.name).filter(Boolean).join(", ") || data.categoryName,
        rating: Number(data.rating || 0).toFixed(2),
        reviews: data.reviewCount || 0,
        price: data.startingPrice ? `${data.currency} ${data.startingPrice}` : t("askForQuote"),
        image: data.mainImageUrl || "photo-1519225421980-715cb0215aed",
        tag: "Verified supplier",
        verified: data.approved,
      };
      if (account?.role === "CUSTOMER" && !(account.countryCodes || []).some((code) => (supplier.countryCodes || []).includes(code))) {
        navigate("/discover", { replace: true });
        setNotice(t("supplierNotInSelectedCountries"));
        return;
      }
      setSelectedSupplier(supplier);
    }).catch(() => setNotice(t("supplierPageUnavailable")));
  }, [account, catalog, directoryLoaded, language, location.pathname, navigate, supplierSlug, t]);
  useEffect(() => {
    let active = true;
    if (!account) {
      setApiBookings(null);
      return () => { active = false; };
    }
    if (account.role === "CUSTOMER" || account.role === "SUPPLIER") {
      bookingsApi.list().then(({ data }) => {
        if (active) {
          setApiBookings(data);
          setBookingsLoadError(false);
        }
      }).catch((error) => {
        if (active) {
          setBookingsLoadError(true);
          setNotice(error.response?.data?.detail || t("errorLoadBookings"));
        }
      });
    }
    if (account.role === "CUSTOMER") {
      checklistApi.list().then(({ data }) => {
        if (!active) return;
        setTasks(data.map((item) => ({
          id: item.id,
          title: item.title,
          due: item.dueDate || t("wheneverReady"),
          category: item.categorySlug || "Custom",
          done: item.completed,
          supplier: item.supplierName,
          contract: item.contractDetails,
          payment: item.paymentStatus,
          budgetAmount: item.budgetAmount,
          dueDate: item.dueDate,
          notes: item.notes,
        })));
        setChecklistLoadError(false);
        setChecklistLoaded(true);
      }).catch((error) => {
        if (!active) return;
        setChecklistLoadError(true);
        setChecklistLoaded(true);
        setNotice(error.response?.data?.detail || t("errorLoadChecklist"));
      });
      collaborationsApi.listPublic().then(({ data }) => {
        if (active) setSelectedCollaborations(data.filter((collaboration) => collaboration.selected));
      }).catch((error) => {
        if (active) setNotice(error.response?.data?.detail || t("errorLoadCollaborations"));
      });
      paymentsApi.history().then(({ data }) => {
        if (active) setPaymentRecords(data);
      }).catch((error) => {
        if (active) setNotice(error.response?.data?.detail || t("errorLoadPaymentHistory"));
      });
    } else {
      setTasks([]);
      setPaymentRecords([]);
    }
    return () => { active = false; };
  }, [account, language]);
  useEffect(() => {
    let active = true;
    seenNotificationIds.current = new Set();
    liveNotifications.current = [];
    setNotifications([]);
    setUnreadNotificationCount(0);
    setNotificationsOpen(false);
    if (!account || !["CUSTOMER", "SUPPLIER"].includes(account.role)) {
      return () => { active = false; };
    }

    notificationsApi.list().then(({ data }) => {
      if (!active) return;
      const fetchedIds = new Set(data.notifications.map((item) => item.id));
      const arrivingDuringLoad = liveNotifications.current.filter((item) => !fetchedIds.has(item.id));
      const merged = [...liveNotifications.current, ...data.notifications]
        .filter((item, index, list) => list.findIndex((candidate) => candidate.id === item.id) === index)
        .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
        .slice(0, 50);
      seenNotificationIds.current = new Set(merged.map((item) => item.id));
      setNotifications(merged);
      setUnreadNotificationCount(data.unreadCount + arrivingDuringLoad.filter((item) => !item.read).length);
    }).catch((error) => {
      if (active) setNotice(error.response?.data?.detail || t("errorLoadNotifications"));
    });

    const token = localStorage.getItem("wedora-token");
    const client = new Client({
      brokerURL: websocketUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        if (!active) return;
        client.subscribe("/user/queue/notifications", (frame) => {
          const incoming = JSON.parse(frame.body);
          if (seenNotificationIds.current.has(incoming.id)) return;
          seenNotificationIds.current.add(incoming.id);
          liveNotifications.current = [incoming, ...liveNotifications.current]
            .filter((item, index, list) => list.findIndex((candidate) => candidate.id === item.id) === index)
            .slice(0, 50);
          setNotifications((current) => [incoming, ...current.filter((item) => item.id !== incoming.id)].slice(0, 50));
          if (!incoming.read) setUnreadNotificationCount((count) => count + 1);
          setNotice(`${t("newNotification")}: ${t(`notification_${incoming.type}`, { actorName: incoming.actorName })}`);
          if (incoming.type !== "NEW_MESSAGE") {
            bookingsApi.list().then(({ data }) => setApiBookings(data)).catch((error) => {
              setNotice(error.response?.data?.detail || t("errorLoadBookings"));
            });
          }
        });
      },
    });
    client.activate();
    return () => {
      active = false;
      client.deactivate();
    };
  }, [account, i18n.language, t]);
  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = language === "ar" ? "rtl" : "ltr";
  }, [language]);
  useEffect(() => {
    if (!notice) return undefined;
    const timer = window.setTimeout(() => setNotice(""), 3200);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const countryCatalog = useMemo(() => {
    if (account?.role !== "CUSTOMER") return catalog;
    const coupleCountries = account.countryCodes || [];
    return catalog.filter((supplier) =>
      coupleCountries.length > 0
      && (supplier.countryCodes || []).some((countryCode) => coupleCountries.includes(countryCode)));
  }, [account, catalog]);

  const filteredSuppliers = useMemo(() => countryCatalog.filter((supplier) => {
    const matchesCategory = selectedCategory === "All" || supplier.category === selectedCategory
      || supplier.categories?.some((category) => category.name === selectedCategory);
    const matchesSaved = activePage !== "saved" || saved.includes(supplier.id);
    const matchesQuery = `${supplier.name} ${supplier.category} ${supplier.place}`.toLowerCase().includes(query.toLowerCase());
    return matchesCategory && matchesSaved && matchesQuery;
  }), [activePage, countryCatalog, query, saved, selectedCategory]);

  const checklistTasks = useMemo(() => {
    const servicesByCategory = new Map();
    selectedCollaborations.forEach((collaboration) => {
      (collaboration.items || []).forEach((item) => {
        if (!item.categorySlug) return;
        const categorySlug = item.categorySlug.toLowerCase();
        const services = servicesByCategory.get(categorySlug) || [];
        if (!services.some((service) => service.collaborationId === collaboration.id && service.serviceId === item.serviceId)) {
          services.push({
            collaborationId: collaboration.id,
            serviceId: item.serviceId,
            collaborationName: collaboration.name,
            serviceName: item.serviceName,
            categoryName: item.categoryName,
          });
        }
        servicesByCategory.set(categorySlug, services);
      });
    });
    const matchedCategories = new Set();
    const withCollaborations = tasks.map((task) => {
      const categorySlug = task.category?.toLowerCase();
      const collaborationSources = servicesByCategory.get(categorySlug) || [];
      if (collaborationSources.length) matchedCategories.add(categorySlug);
      return {
        ...task,
        done: task.done || collaborationSources.length > 0,
        collaborationSources,
      };
    });
    servicesByCategory.forEach((collaborationSources, categorySlug) => {
      if (matchedCategories.has(categorySlug)) return;
      const category = directoryCategories.find((item) => item.slug === categorySlug);
      withCollaborations.push({
        id: `collaboration-category-${categorySlug}`,
        title: category?.name || collaborationSources[0]?.categoryName || categorySlug,
        due: t("wheneverReady"),
        category: categorySlug,
        done: true,
        budgetAmount: null,
        collaborationSources,
        derivedFromCollaboration: true,
      });
    });
    return withCollaborations;
  }, [tasks, selectedCollaborations, directoryCategories, t]);

  const completedTasks = checklistTasks.filter((task) => task.done).length;
  const progress = Math.round((completedTasks / Math.max(checklistTasks.length, 1)) * 100);

  function toggleSaved(id) {
    if (account?.role !== "CUSTOMER") {
      setNotice(t("signInCoupleToSave"));
      if (!account) {
        setAuthMode("login");
        setAuthOpen(true);
      }
      return;
    }
    setSaved((current) => {
      const next = current.includes(id) ? current.filter((value) => value !== id) : [...current, id];
      localStorage.setItem(`wedora-saved-${account.id}`, JSON.stringify(next));
      return next;
    });
  }
  async function saveCountryPreferences(countryCodes) {
    try {
      const { data } = await usersApi.updateCountries(countryCodes);
      localStorage.setItem("wedora-token", data.token);
      localStorage.setItem("wedora-user", JSON.stringify(data));
      setAccount(data);
      setNotice(t("countryPreferencesSaved"));
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorSaveCountryPreferences"));
    }
  }
  async function saveWeddingDate(weddingDate) {
    try {
      const { data } = await usersApi.updateWeddingDate(weddingDate || null);
      localStorage.setItem("wedora-token", data.token);
      localStorage.setItem("wedora-user", JSON.stringify(data));
      setAccount(data);
      setNotice(t("weddingDateSaved"));
      return true;
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorSaveWeddingDate"));
      return false;
    }
  }
  async function saveCurrencyPreference(currencyCode) {
    try {
      const { data } = await usersApi.updateCurrency(currencyCode);
      localStorage.setItem("wedora-token", data.token);
      localStorage.setItem("wedora-user", JSON.stringify(data));
      setAccount(data);
      setNotice(t("currencyPreferenceSaved"));
      return true;
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorSaveCurrencyPreference"));
      return false;
    }
  }
  async function savePhoneNumber(phoneNumber) {
    try {
      const { data } = await usersApi.updatePhoneNumber(phoneNumber || null);
      localStorage.setItem("wedora-token", data.token);
      localStorage.setItem("wedora-user", JSON.stringify(data));
      setAccount(data);
      setNotice(t("whatsappPhoneSaved"));
      return true;
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorSavePhoneNumber"));
      return false;
    }
  }
  function openSupplier(supplier) {
    setSelectedSupplier(supplier);
    navigate(`/supplier/${encodeURIComponent(supplier.slug || supplier.id)}`, {
      state: { returnPage: activePage, returnPath: location.pathname },
    });
  }
  function closeSupplier() {
    setSelectedSupplier(null);
    if (supplierRoute) navigate(returnPath);
  }
  function changeLanguage(value) {
    setLanguage(value);
    i18n.changeLanguage(value);
    localStorage.setItem("wedora-language", value);
  }
  async function bookSupplier(supplier, details) {
    if (account?.role !== "CUSTOMER") {
      setBookingSupplier(null);
      if (!account) {
        setAuthMode("login");
        setAuthOpen(true);
        setNotice(t("signInToBook"));
      } else {
        setNotice(t("coupleBookingOnly"));
      }
      return;
    }
    try {
      const { data: booking } = await bookingsApi.request({
        supplierId: supplier.id,
        serviceOfferingId: details.serviceOfferingId || null,
        eventDate: details.eventDate,
        notes: details.notes,
        quantity: details.quantity || null,
      });
      setApiBookings((current) => [booking, ...(current || [])]);
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorSendBookingRequest"));
      return;
    }
    setTasks((current) => current.map((task) =>
      task.category.toLowerCase() === supplier.category.toLowerCase()
        ? { ...task, done: false, supplier: supplier.name, contract: t("bookingRequestSentStatus"), payment: t("awaitingSupplierQuote") }
        : task,
    ));
    setBookingSupplier(null);
    setSelectedSupplier(null);
    setNotice(t("bookingRequestSent", { name: supplier.name }));
    setActivePage("bookings");
  }
  function toggleTask(id) {
    if (account?.role !== "CUSTOMER") return;
    const task = tasks.find((item) => item.id === id);
    if (!task) return;
    const completed = !task.done;
    setTasks((current) => current.map((item) => item.id === id ? { ...item, done: completed } : item));
    if (account?.role === "CUSTOMER") {
      checklistApi.setCompleted(id, completed).catch((error) => {
        setTasks((current) => current.map((item) => item.id === id ? { ...item, done: task.done } : item));
        setNotice(error.response?.data?.detail || t("errorUpdateChecklist"));
      });
    }
  }
  async function saveChecklistTask(title, categorySlug, budgetAmount) {
    if (account?.role !== "CUSTOMER") return false;
    const request = {
      title,
      categorySlug,
      dueDate: checklistEditingTask?.dueDate || null,
      notes: checklistEditingTask?.notes || null,
      budgetAmount,
    };
    try {
      const { data } = checklistEditingTask
        ? await checklistApi.update(checklistEditingTask.id, request)
        : await checklistApi.add(request);
      const updatedTask = {
        id: data.id, title: data.title, due: data.dueDate || t("wheneverReady"),
        category: data.categorySlug || "Custom", done: data.completed,
        supplier: data.supplierName, contract: data.contractDetails, payment: data.paymentStatus,
        budgetAmount: data.budgetAmount, dueDate: data.dueDate, notes: data.notes,
      };
      setTasks((current) => checklistEditingTask
        ? current.map((task) => task.id === data.id ? updatedTask : task)
        : [...current, updatedTask]);
      setChecklistModalOpen(false);
      setChecklistEditingTask(null);
      setNotice(checklistEditingTask ? t("checklistTaskUpdated") : t("checklistTaskAdded"));
      return true;
    } catch (error) {
      setNotice(error.response?.data?.detail || t(checklistEditingTask ? "errorUpdateChecklistTask" : "errorAddChecklistTask"));
      return false;
    }
  }

  async function removeChecklistTask(id) {
    try {
      await checklistApi.remove(id);
      setTasks((current) => current.filter((item) => item.id !== id));
      setNotice(t("checklistTaskRemoved"));
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorRemoveChecklistTask"));
    }
  }

  function signOut() {
    localStorage.removeItem("wedora-token");
    localStorage.removeItem("wedora-user");
    setAccount(null);
    setSaved([]);
    setTasks([]);
    setApiBookings(null);
    setNotifications([]);
    setUnreadNotificationCount(0);
    setBookingsLoadError(false);
    setPaymentRecords([]);
    setAuthOpen(false);
    navigate("/");
    setNotice(t("signedOut"));
  }
  async function openNotification(notification) {
    setNotificationsOpen(false);
    if (!notification.read) {
      try {
        await notificationsApi.markRead(notification.id);
        setNotifications((current) => current.map((item) =>
          item.id === notification.id ? { ...item, read: true } : item));
        setUnreadNotificationCount((count) => Math.max(0, count - 1));
      } catch (error) {
        setNotice(error.response?.data?.detail || t("errorUpdateNotification"));
        return;
      }
    }
    if (notification.collaborationId) {
      setActivePage("collaborations");
      return;
    }
    try {
      const { data } = await bookingsApi.list();
      setApiBookings(data);
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorLoadBookings"));
    }
    setFocusedMessageBookingId(notification.bookingId);
    setActivePage(notification.type === "NEW_MESSAGE" ? "messages" : "bookings");
  }
  async function markAllNotificationsRead() {
    try {
      await notificationsApi.markAllRead();
      setNotifications((current) => current.map((item) => ({ ...item, read: true })));
      setUnreadNotificationCount(0);
    } catch (error) {
      setNotice(error.response?.data?.detail || t("errorUpdateNotification"));
    }
  }

  const roleNavigation = account?.role === "CUSTOMER"
    ? customerNavigation
    : account?.role === "SUPPLIER"
      ? [
        { id: "supplierDashboard", icon: BriefcaseBusiness },
        ...(supplierCollaborationEligible ? [{ id: "collaborations", icon: Users }] : []),
        { id: "bookings", icon: CalendarDays },
        { id: "messages", icon: MessageCircle },
        { id: "discover", icon: Search },
      ]
      : account?.role === "ADMIN" ? [{ id: "admin", icon: ShieldCheck }] : [];
  const roleLabel = account?.role === "CUSTOMER" ? t("coupleAccount")
    : account?.role === "SUPPLIER" ? t("supplierAccount")
      : account?.role === "ADMIN" ? t("administrator") : "";
  const pageTitle = activePage === "home" ? t("home")
    : activePage === "supplierDashboard" ? t("businessDashboard")
      : activePage === "admin" ? t("admin") : t(activePage);
  const hasPageAccess = account
    ? Boolean(workspacePages[account.role]?.includes(activePage)
      && !(account.role === "SUPPLIER" && activePage === "collaborations" && !supplierCollaborationEligible))
    : activePage === "discover";

  return (
    <div className={`app-shell${darkMode ? " dark" : ""}`}
      data-palette={palette} data-typography={typography} data-density={density}>
      {!isPublicLanding && account && <aside className={`sidebar${mobileNav ? " mobile-open" : ""}`}>
        <button className="brand" onClick={() => setActivePage("home")} aria-label={t("wedoraHome")}>
          <BrandContent />
        </button>
        <div className="sidebar-label">{t("yourWorkspace").toUpperCase()}</div>
        <div className="wedding-switcher account-switcher">
          <div className="couple-avatar">{account?.name?.slice(0, 1)?.toUpperCase() || "W"}</div>
          <div className="wedding-meta"><strong>{account?.name || account?.email}</strong><small>{roleLabel}</small></div>
        </div>
        <div className="sidebar-label nav-caption">{account?.role === "CUSTOMER" ? t("planYourDay").toUpperCase() : t("yourWorkspace").toUpperCase()}</div>
        <nav className="main-nav" aria-label={t("mainNavigation")}>
          {roleNavigation.map(({ id, icon: Icon }) => (
            <button key={id} onClick={() => { setActivePage(id); setMobileNav(false); }} className={`nav-link${activePage === id ? " active" : ""}`}>
              <Icon size={18} strokeWidth={1.8} /><span>{t(id)}</span>
            </button>
          ))}
        </nav>
        {account?.role !== "ADMIN" && <div className="sidebar-card">
          <div className="sidebar-card-icon"><Sparkles size={17} /></div>
          <strong>{account?.role === "SUPPLIER" ? t("exploreMarketplace") : t("littleMoreMagic")}</strong>
          <p>{account?.role === "SUPPLIER" ? t("supplierMarketplacePreview") : t("findWeddingPro")}</p>
          <button onClick={() => { setActivePage("discover"); setSelectedCategory("All"); }} className="sidebar-discover">{t("browseSuppliers")} <ArrowRight size={14} /></button>
        </div>}
        <div className="sidebar-bottom">
          <div className="profile-row account-profile"><div className="profile-avatar">{account?.name?.slice(0, 1)?.toUpperCase() || "W"}</div><div><strong>{account?.name || account?.email}</strong><small>{roleLabel}</small></div></div>
          <Link className="privacy-sidebar-link" to="/privacy-policy">{t("privacyPolicy")}</Link>
          <button className="logout-button sidebar-logout" onClick={signOut}><LogOut size={16} /><span>{t("signOut")}</span></button>
        </div>
      </aside>}

      <main className={`main-content${isPublicLanding ? " public-main" : ""}`}>
        {isPublicLanding ? <header className="public-header">
          <button className="brand" onClick={() => navigate("/")} aria-label={t("wedoraHome")}>
            <BrandContent />
          </button>
          <nav className="public-nav" aria-label={t("mainNavigation")}>
            <button onClick={() => setActivePage("discover")}>{t("landingExploreNav")}</button>
            <a href="#how-it-works">{t("landingHowNav")}</a>
            <AppearanceControl appearance={appearance} onChange={setAppearance} />
            <select className="language-select" value={language} onChange={(event) => changeLanguage(event.target.value)} aria-label={t("chooseLanguage")}>
              <option value="en">EN</option><option value="fr">FR</option><option value="el">EL</option><option value="ar">عربي</option>
            </select>
            <button className="public-sign-in" onClick={() => {
              if (account) {
                navigate(account.role === "ADMIN" ? "/admin" : account.role === "SUPPLIER" ? "/supplierDashboard" : "/home");
              } else {
                setAuthMode("login");
                setAuthOpen(true);
              }
            }}>
              {account ? t("landingWorkspace") : t("landingSignIn")}
            </button>
            <button className="primary-button public-cta" onClick={() => {
              if (account) {
                navigate(account.role === "ADMIN" ? "/admin" : account.role === "SUPPLIER" ? "/supplierDashboard" : "/home");
              } else {
                setAuthMode("register");
                setAuthOpen(true);
              }
            }}>{account ? t("landingOpenWorkspace") : t("landingGetStarted")} <ArrowRight size={15} /></button>
          </nav>
        </header> : <header className="topbar">
          <button className="mobile-menu icon-button" onClick={() => setMobileNav(!mobileNav)} aria-label={t("openMenu")}><Menu size={20} /></button>
          <div className="breadcrumbs"><button className="breadcrumbs-home" onClick={() => setActivePage("home")}>{t("wedoraHome")}</button><ChevronRight size={14} /><strong>{pageTitle}</strong></div>
          <div className="topbar-actions">
            <label className="search-box"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("search")} /><kbd>⌘ K</kbd></label>
            <AppearanceControl appearance={appearance} onChange={setAppearance} />
            <select className="language-select" value={language} onChange={(event) => changeLanguage(event.target.value)} aria-label={t("chooseLanguage")}>
              <option value="en">EN</option><option value="fr">FR</option><option value="el">EL</option><option value="ar">عربي</option>
            </select>
            <button className="icon-button theme-toggle" onClick={() => setAppearance((current) => ({ ...current, darkMode: !current.darkMode }))} aria-label={t("toggleDarkMode")}>{darkMode ? <Sun size={18} /> : <Moon size={18} />}</button>
            <div className="notification-menu">
              <button className="icon-button notification-button" aria-label={t("notifications")}
                aria-expanded={notificationsOpen} onClick={() => setNotificationsOpen((open) => !open)}>
                <Bell size={18} />
                {unreadNotificationCount > 0 && <span className="notification-count">{unreadNotificationCount > 99 ? "99+" : unreadNotificationCount}</span>}
              </button>
              {notificationsOpen && <section className="notification-popover" aria-label={t("notifications")}>
                <div className="notification-popover-header">
                  <strong>{t("notifications")}</strong>
                  {unreadNotificationCount > 0 && <button onClick={markAllNotificationsRead}>{t("markAllRead")}</button>}
                </div>
                {notifications.length ? <div className="notification-list">
                  {notifications.map((item) => <button key={item.id}
                    className={`notification-item${item.read ? "" : " unread"}`}
                    onClick={() => openNotification(item)}>
                    <span className="notification-item-icon"><Bell size={15} /></span>
                    <span className="notification-item-copy">
                      <strong>{t(`notification_${item.type}`, { actorName: item.actorName })}</strong>
                      <small>{item.eventDate ? `${item.eventDate} · ` : ""}{new Date(item.createdAt).toLocaleString(language)}</small>
                    </span>
                    {!item.read && <i aria-label={t("unreadNotification")} />}
                  </button>)}
                </div> : <p className="notification-empty">{t("noNewNotifications")}</p>}
              </section>}
            </div>
            {account
              ? <button className="logout-button topbar-logout" aria-label={t("signOut")} onClick={signOut}><LogOut size={15} /><span>{t("signOut")}</span></button>
              : <button className="logout-button topbar-logout" onClick={() => { setAuthMode("login"); setAuthOpen(true); }}>{t("landingSignIn")}</button>}
          </div>
        </header>}

        <div className={`page-wrap${isPublicLanding ? " public-page-wrap" : ""}`}>
          {isPublicLanding && <PublicLanding
            t={t}
            account={account}
            categories={directoryCategories}
            suppliers={catalog}
            countryOptions={countryOptions}
            saved={saved}
            onExplore={() => { setActivePage("discover"); setSelectedCategory("All"); }}
            onSignIn={() => { setAuthMode("login"); setAuthOpen(true); }}
            onRegister={() => { setAuthMode("register"); setAuthOpen(true); }}
            onSupplierApply={() => { setAuthMode("supplier"); setAuthOpen(true); }}
            onSave={toggleSaved}
            onOpenSupplier={openSupplier}
          />}
          {activePage === "home" && !isPublicLanding && account?.role === "CUSTOMER" && !account.countryCodes?.length && <CountryPreferences countryOptions={countryOptions} countryLoadStatus={countryLoadStatus} onRetryCountries={retryCountryLoad} countryCodes={account.countryCodes || []} onSave={saveCountryPreferences} />}
          {activePage === "home" && !isPublicLanding && account?.role === "CUSTOMER" && (
            <CoupleDashboard
              account={account}
              tasks={checklistTasks}
              suppliers={countryCatalog}
              directoryLoaded={directoryLoaded}
              directoryError={directoryError}
              checklistLoaded={checklistLoaded}
              checklistLoadError={checklistLoadError}
              bookings={apiBookings}
              bookingsLoadError={bookingsLoadError}
              savedCount={saved.length}
              onOpenChecklist={() => setActivePage("checklist")}
              onOpenBookings={() => setActivePage("bookings")}
              onDiscover={() => { setActivePage("discover"); setSelectedCategory("All"); }}
              onSaveSupplier={toggleSaved}
              savedSuppliers={saved}
              onOpenSupplier={openSupplier}
              onSaveWeddingDate={saveWeddingDate}
              onSaveCurrencyPreference={saveCurrencyPreference}
            />
          )}

          {activePage !== "home" && hasPageAccess && <div className="page-title-row"><div><div className="eyebrow">{account?.role === "CUSTOMER" ? t("weddingYourWay") : roleLabel ? roleLabel.toUpperCase() : t("weddingProfessionals")}</div><h1>{pageTitle}</h1><p>{activePage === "checklist" ? t("checklistIntro") : activePage === "discover" ? t("discoverIntro") : activePage === "collaborations" ? t("collaborationsIntro") : activePage === "invitations" ? t("invitationsIntro") : t("pageIntro")}</p></div>
            {activePage === "checklist" && <button className="primary-button" onClick={() => { setChecklistEditingTask(null); setChecklistModalOpen(true); }}><span>+</span> {t("addTask")}</button>}
            {activePage === "discover" && <button className="secondary-button" onClick={() => setNotice(t("showingAllSuppliers"))}><SlidersHorizontal size={16} /> {t("filters")} <span className="filter-dot" /></button>}
          </div>}

          {activePage === "discover" && hasPageAccess && account?.role === "CUSTOMER" && <CountryPreferences countryOptions={countryOptions} countryLoadStatus={countryLoadStatus} onRetryCountries={retryCountryLoad} countryCodes={account.countryCodes || []} onSave={saveCountryPreferences} />}
          {activePage === "discover" && hasPageAccess && <><div className="filter-chips">{["All", ...directoryCategories.map(({ name }) => name)].map((category) => <button key={category} className={selectedCategory === category ? "filter-chip selected" : "filter-chip"} onClick={() => setSelectedCategory(category)}>{category === "All" ? t("allCategories") : supplierCategoryLabel(t, category)}</button>)}</div>
            <div className="results-caption"><span>{t("supplierFinds", { count: filteredSuppliers.length })}</span><button onClick={() => setNotice(t("sortedByRecommended"))}>{t("recommendedSort")} <ChevronDown size={15} /></button></div>
            {filteredSuppliers.length ? <div className="supplier-grid discover-grid">{filteredSuppliers.map((supplier) => <SupplierCard key={supplier.id} supplier={supplier} saved={saved.includes(supplier.id)} onSave={toggleSaved} onOpen={openSupplier} />)}</div> : <EmptyState title={t("noSuppliersTitle")} detail={account?.role === "CUSTOMER" && !account.countryCodes?.length ? t("chooseCountryBeforeBrowsing") : t("noSuppliersDetail")} action={() => { setQuery(""); setSelectedCategory("All"); }} actionLabel={t("browseSuppliers")} />}
          </>}

          {activePage === "saved" && account?.role === "CUSTOMER" && <>{filteredSuppliers.length ? <div className="supplier-grid discover-grid">{filteredSuppliers.map((supplier) => <SupplierCard key={supplier.id} supplier={supplier} saved={saved.includes(supplier.id)} onSave={toggleSaved} onOpen={openSupplier} />)}</div> : <EmptyState title={t("savedSuppliersEmptyTitle")} detail={t("savedSuppliersEmptyDetail")} action={() => setActivePage("discover")} actionLabel={t("browseSuppliers")} />}</>}

          {activePage === "checklist" && account?.role === "CUSTOMER" && (
            checklistLoadError
              ? <EmptyState title={t("checklistLoadFailedTitle")} detail={t("tryAgainShortly")} action={() => window.location.reload()} actionLabel={t("reloadChecklist")} />
              : !checklistLoaded
                ? <p className="workspace-loading">{t("loadingChecklist")}</p>
                : <div className="checklist-layout"><ChecklistCard tasks={checklistTasks} categories={directoryCategories} progress={progress} currencyCode={account.currencyCode || "USD"} onToggle={toggleTask} onEdit={(task) => { setChecklistEditingTask(task); setChecklistModalOpen(true); }} onRemove={removeChecklistTask} expanded /><aside className="side-tip"><span className="tip-icon"><Sparkles size={18} /></span><h3>{t("oneStepAtTime")}</h3><p>{t("checklistTip")}</p><div className="tip-count">{t("tasksDone", { completed: completedTasks, total: checklistTasks.length })} <span>{progress}%</span></div><div className="progress-track"><span style={{ width: `${progress}%` }} /></div></aside></div>
          )}

          {activePage === "guests" && account?.role === "CUSTOMER" && <GuestsWorkspace account={account} countryOptions={countryOptions} onNotice={setNotice} />}
          {activePage === "invitations" && account?.role === "CUSTOMER" && <InvitationsWorkspace account={account} onNotice={setNotice} onOpenGuests={() => setActivePage("guests")} />}

          {activePage === "bookings" && (account?.role === "CUSTOMER" || account?.role === "SUPPLIER") && <div className="bookings-panel"><div className="panel-heading"><div><h2>{account.role === "SUPPLIER" ? t("bookingEnquiries") : t("yourSupplierRequests")}</h2><p>{account.role === "SUPPLIER" ? t("manageBookingRequests") : t("bookingConversationSummary")}</p></div><CalendarDays size={20} /></div>
            {apiBookings !== null ? <div className="booking-list">{apiBookings.map((booking) => <BookingRow key={booking.id} booking={booking} role={account.role} payment={paymentRecords.find((payment) => payment.bookingId === booking.id)} reviewed={reviewedBookingIds.includes(booking.id)} onNotice={setNotice} onDetails={() => setBookingDetails(booking)} onPay={() => setPaymentBookingId(booking.id)} onReview={() => setReviewBooking(booking)} onUpdated={(updated) => setApiBookings((items) => items.map((item) => item.id === updated.id ? updated : item))} onConfirmed={(updated) => {
              if (account.role === "CUSTOMER") {
                setTasks((items) => items.map((task) => task.category.toLowerCase() === updated.categorySlug
                  ? { ...task, done: true, supplier: updated.supplierName, contract: "Booking confirmed", payment: "Payment due" }
                  : task));
              }
            }} />)}
              {!apiBookings.length && <EmptyState
                title={account.role === "SUPPLIER" ? t("noBookingEnquiries") : t("noSupplierRequests")}
                detail={account.role === "SUPPLIER" ? t("newCoupleRequestsHere") : t("discoverAndRequest")}
                action={() => setActivePage(account.role === "SUPPLIER" ? "supplierDashboard" : "discover")}
                actionLabel={account.role === "SUPPLIER" ? t("businessDashboard") : t("browseSuppliers")}
              />}
            </div> : <p className="workspace-loading">{bookingsLoadError ? t("bookingLoadFailed") : t("loadingBookingRequests")}</p>}
            {account.role === "CUSTOMER" && apiBookings?.length > 0 && <div className="booking-help"><CircleHelp size={17} /> {t("paymentAfterConfirmation")} <button onClick={() => setNotice(t("paymentsStripeNotice"))}>{t("howPaymentsWork")} <ArrowRight size={13} /></button></div>}
          </div>}

          {activePage === "messages" && (account?.role === "CUSTOMER" || account?.role === "SUPPLIER") && account && apiBookings?.length > 0
            ? <ChatInbox bookings={apiBookings} user={account} initialBookingId={focusedMessageBookingId} onNotice={setNotice} />
            : activePage === "messages" && (account?.role === "CUSTOMER" || account?.role === "SUPPLIER") && <div className="messages-empty"><div className="messages-illustration"><MessageCircle size={38} /><span>✿</span></div><div className="eyebrow">{t("yourWorkspace").toUpperCase()}</div><h2>{t("messagesEmptyTitle")}</h2><p>{account.role === "SUPPLIER" ? t("supplierMessagesEmpty") : t("customerMessagesEmpty")}</p><button className="primary-button" onClick={() => setActivePage(account.role === "SUPPLIER" ? "bookings" : "discover")}>{account.role === "SUPPLIER" ? t("viewBookingEnquiries") : t("meetSuppliers")} <ArrowRight size={15} /></button></div>}
          {activePage === "collaborations" && account && (account.role === "CUSTOMER" || (account.role === "SUPPLIER" && supplierCollaborationEligible)) && <CollaborationsWorkspace role={account.role} onNotice={setNotice} onSelectedCollaborations={setSelectedCollaborations} />}
          {activePage === "supplierDashboard" && account?.role === "SUPPLIER" && <SupplierDashboard countryOptions={countryOptions} countryLoadStatus={countryLoadStatus} onRetryCountries={retryCountryLoad} categories={directoryCategories} onNotice={setNotice} bookings={apiBookings} />}
          {activePage === "admin" && account?.role === "ADMIN" && <AdminDashboard onNotice={setNotice} />}
        </div>
      </main>

      {selectedSupplier && <SupplierModal supplier={selectedSupplier} countryOptions={countryOptions} saved={saved.includes(selectedSupplier.id)} canContact={account?.role === "CUSTOMER"} onSave={toggleSaved} onClose={closeSupplier} onBook={(offering = null, options = []) => setBookingSupplier({ supplier: selectedSupplier, offering, options })} onMessage={() => { setNotice(t("supplierConversationReady", { name: selectedSupplier.name })); closeSupplier(); setActivePage("messages"); }} />}
      {bookingSupplier && <BookingModal supplier={bookingSupplier.supplier} serviceOffering={bookingSupplier.offering} options={bookingSupplier.options} onClose={() => setBookingSupplier(null)} onSubmit={(details) => bookSupplier(bookingSupplier.supplier, details)} />}
      {authOpen && <AuthModal key={authMode} initialMode={authMode} account={account} countryOptions={countryOptions} countryLoadStatus={countryLoadStatus} onRetryCountries={retryCountryLoad} onSavePhoneNumber={savePhoneNumber} onClose={() => setAuthOpen(false)} onSignedIn={(user) => {
        localStorage.setItem("wedora-token", user.token);
        localStorage.setItem("wedora-user", JSON.stringify(user));
        setSaved(user.role === "CUSTOMER" ? readSavedSuppliers(user.id) : []);
        setAccount(user);
        setAuthOpen(false);
        navigate(workspacePathFor(user));
        setNotice(t("welcomeSignedIn", { name: user.name }));
      }} onSignOut={signOut} />}
      {paymentBookingId && <PaymentModal bookingId={paymentBookingId} onClose={() => setPaymentBookingId(null)} onPaid={async () => {
        setPaymentBookingId(null);
        try {
          const [payments, bookings] = await Promise.all([paymentsApi.history(), bookingsApi.list()]);
          setPaymentRecords(payments.data);
          setApiBookings(bookings.data);
          setNotice(t("paymentSubmitted"));
        } catch (error) {
          setNotice(error.response?.data?.detail || t("errorRefreshPayments"));
        }
      }} />}
      {bookingDetails && <BookingDetailsModal booking={bookingDetails} role={account?.role} onClose={() => setBookingDetails(null)} onQuote={() => {
        setQuoteBooking(bookingDetails);
        setBookingDetails(null);
      }} onWorkflow={async (action, comment) => {
        try {
          const { data } = await bookingsApi[action](bookingDetails.id, comment);
          setApiBookings((items) => items.map((item) => item.id === data.id ? data : item));
          setBookingDetails(data);
          return true;
        } catch (error) {
          setNotice(error.response?.data?.detail || t("errorUpdateRequest"));
          return false;
        }
      }} onAccept={async () => {
        try {
          const { data } = await bookingsApi.accept(bookingDetails.id);
          setApiBookings((items) => items.map((item) => item.id === data.id ? data : item));
          setBookingDetails(null);
          setNotice(t("requestAcceptedOfferReady"));
        } catch (error) {
          setNotice(error.response?.data?.detail || t("errorAcceptRequest"));
        }
      }} onDecline={async () => {
        try {
          const { data } = await bookingsApi.decline(bookingDetails.id);
          setApiBookings((items) => items.map((item) => item.id === data.id ? data : item));
          setBookingDetails(null);
          setNotice(t("bookingRequestDeclined"));
        } catch (error) {
          setNotice(error.response?.data?.detail || t("errorDeclineRequest"));
        }
      }} />}
      {checklistModalOpen && <ChecklistModal key={checklistEditingTask?.id || "new-checklist-task"} task={checklistEditingTask} categories={directoryCategories} currencyCode={account?.currencyCode || "USD"} onClose={() => { setChecklistModalOpen(false); setChecklistEditingTask(null); }} onSubmit={saveChecklistTask} />}
      {quoteBooking && <QuoteModal booking={quoteBooking} onClose={() => setQuoteBooking(null)} onSubmit={async (quote) => {
        try {
          const { data } = await bookingsApi.quote(quoteBooking.id, quote);
          setApiBookings((items) => items.map((item) => item.id === data.id ? data : item));
          setQuoteBooking(null);
          setNotice(t("quoteSent"));
        } catch (error) {
          setNotice(error.response?.data?.detail || t("errorSendQuote"));
        }
      }} />}
      {reviewBooking && <ReviewModal booking={reviewBooking} onClose={() => setReviewBooking(null)} onSubmit={async (review) => {
        try {
          await reviewsApi.create(reviewBooking.id, review);
          setReviewedBookingIds((ids) => [...ids, reviewBooking.id]);
          setReviewBooking(null);
          setNotice(t("thankYouReview"));
        } catch (error) {
          setNotice(error.response?.data?.detail || t("errorSendReview"));
        }
      }} />}
      {notice && <div role="status" className="toast"><CheckCircle2 size={18} />{notice}<button onClick={() => setNotice("")} aria-label={t("dismiss")}><X size={15} /></button></div>}
      {account?.role === "CUSTOMER" && !isPublicLanding && <WeddingAssistant />}
    </div>
  );
}

function PublicLanding({ t, account, categories, suppliers, saved, onExplore, onSignIn, onRegister, onSupplierApply, onSave, onOpenSupplier }) {
  const roleCards = [
    {
      icon: Users,
      audience: t("landingCouples"),
      title: t("landingCouplesTitle"),
      description: t("landingCouplesDetail"),
      action: t("landingCouplesAction"),
      onAction: onRegister,
    },
    {
      icon: BriefcaseBusiness,
      audience: t("landingSuppliers"),
      title: t("landingSuppliersTitle"),
      description: t("landingSuppliersDetail"),
      action: t("landingSuppliersAction"),
      onAction: onSupplierApply,
    },
    {
      icon: ShieldCheck,
      audience: t("landingAdmins"),
      title: t("landingAdminsTitle"),
      description: t("landingAdminsDetail"),
      action: t("landingAdminsAction"),
      onAction: onSignIn,
    },
  ];

  return <div className="public-site">
    <section className="public-hero">
      <div className="public-hero-copy">
        <div className="eyebrow"><span className="eyebrow-dot" /> {t("landingEyebrow")}</div>
        <h1>{t("landingTitle")} <em>{t("landingTitleAccent")}</em></h1>
        <p>{t("landingIntro")}</p>
        <div className="public-hero-actions">
          <button className="primary-button" onClick={onExplore}>{t("landingExplore")} <ArrowRight size={15} /></button>
          <button className="public-text-button" onClick={onSignIn}>{t("landingHaveAccount")}</button>
        </div>
        <div className="public-trust-note"><ShieldCheck size={16} /> {t("landingTrust")}</div>
      </div>
      <div className="public-hero-visual">
        <img src={imageUrl("photo-1519225421980-715cb0215aed", 1100)} alt={t("weddingReceptionAlt")} />
        <div className="public-photo-caption"><span><Sparkles size={14} /> {t("weddingYourWay").toUpperCase()}</span><strong>{t("publicHeroCaption")}</strong></div>
        <div className="public-floating-card"><span className="public-floating-icon"><CheckCircle2 size={17} /></span><span><strong>{t("moreOrganized")}</strong><small>{t("firstIdeaToFinalDetail")}</small></span></div>
      </div>
    </section>

    <section className="public-role-section" aria-labelledby="public-role-heading">
      <div className="public-section-heading">
        <div><div className="eyebrow">{t("landingRoleEyebrow")}</div><h2 id="public-role-heading">{t("landingRoleHeading")}</h2></div>
        <p>{t("landingRoleIntro")}</p>
      </div>
      <div className="public-role-grid">
        {roleCards.map(({ icon: Icon, audience, title, description, action, onAction }, index) => (
          <article className={`public-role-card public-role-${index + 1}`} key={audience}>
            <div className="public-role-icon"><Icon size={20} strokeWidth={1.8} /></div>
            <span className="eyebrow">{audience}</span>
            <h3>{title}</h3>
            <p>{description}</p>
            <button onClick={onAction}>{action} <ArrowRight size={14} /></button>
          </article>
        ))}
      </div>
    </section>

    <section className="public-directory-section">
      <div className="public-section-heading">
        <div><div className="eyebrow">{t("landingDirectoryEyebrow")}</div><h2>{t("landingDirectoryHeading")}</h2></div>
        <button className="public-outline-button" onClick={onExplore}>{t("landingBrowseDirectory")} <ArrowRight size={15} /></button>
      </div>
      <div className="public-category-grid">
        {categories.slice(0, 6).map((category) => <button key={category.name} className={`category-card tone-${category.tone}`} onClick={onExplore}>
          <span className="category-symbol">{category.icon}</span><span className="category-title">{supplierCategoryLabel(t, category.name)}</span>
          <span className="category-count">{t("landingExploreProfessionals")}</span><ArrowRight size={15} className="category-arrow" />
        </button>)}
      </div>
      <div className="public-featured-heading"><div><div className="eyebrow">{t("landingFeaturedEyebrow")}</div><h3>{t("landingFeaturedHeading")}</h3></div><button className="text-button" onClick={onExplore}>{t("landingViewDirectory")} <ArrowRight size={15} /></button></div>
      <div className="supplier-grid">{suppliers.slice(0, 3).map((supplier) => <SupplierCard key={supplier.id} supplier={supplier} saved={saved.includes(supplier.id)} onSave={onSave} onOpen={onOpenSupplier} />)}</div>
    </section>

    <section className="public-how-section" id="how-it-works">
      <div className="public-section-heading">
        <div><div className="eyebrow">{t("landingHowEyebrow")}</div><h2 id="public-how-heading">{t("landingHowHeading")}</h2></div>
      </div>
      <div className="public-steps">
        <article><span>01</span><h3>{t("landingStepExplore")}</h3><p>{t("landingStepExploreDetail")}</p></article>
        <article><span>02</span><h3>{t("landingStepConnect")}</h3><p>{t("landingStepConnectDetail")}</p></article>
        <article><span>03</span><h3>{t("landingStepPlan")}</h3><p>{t("landingStepPlanDetail")}</p></article>
      </div>
    </section>

    <footer className="public-footer">
      <button className="brand" aria-label={t("wedoraHome")} onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}>
        <BrandContent />
      </button>
      <p>{t("landingFooter")}</p>
      <Link className="public-footer-login" to="/privacy-policy">{t("privacyPolicy")}</Link>
      <button className="public-footer-login" onClick={account ? onExplore : onSignIn}>{account ? t("landingFooterExplore") : t("landingSignIn")} <ArrowRight size={14} /></button>
    </footer>
  </div>;
}

function CoupleDashboard({ account, tasks, suppliers, directoryLoaded, directoryError, checklistLoaded, checklistLoadError, bookings, bookingsLoadError, savedCount, savedSuppliers, onOpenChecklist, onOpenBookings, onDiscover, onSaveSupplier, onOpenSupplier, onSaveWeddingDate, onSaveCurrencyPreference }) {
  const { t } = useTranslation();
  const completed = tasks.filter((task) => task.done).length;
  const progress = tasks.length ? Math.round((completed / tasks.length) * 100) : 0;
  const outstandingTasks = tasks.filter((task) => !task.done).slice(0, 3);
  const firstName = account?.name?.trim().split(/\s+/)[0] || t("there");

  return <div className="couple-dashboard">
    <section className="workspace-welcome">
      <div>
        <div className="eyebrow"><span className="eyebrow-dot" /> {t("privatePlanningSpace")}</div>
        <h1>{t("welcomeBack", { name: firstName })}</h1>
        <p>{t("plansTogether")}</p>
      </div>
      <button className="primary-button" onClick={onDiscover}>{t("discoverSuppliers")} <ArrowRight size={15} /></button>
    </section>

    <WeddingDateCard weddingDate={account.weddingDate} onSave={onSaveWeddingDate} />
    <CurrencyPreferenceCard currencyCode={account.currencyCode || "USD"} onSave={onSaveCurrencyPreference} />

    <section className="workspace-metrics" aria-label={t("weddingPlanningSummary")}>
      <article><span><CalendarDays size={16} /> {t("bookingRequests")}</span><strong>{bookings === null ? "—" : bookings.length}</strong><small>{bookingsLoadError ? t("unableLoadRequests") : t("basedOnAccount")}</small></article>
      <article><span><ListChecks size={16} /> {t("checklistProgress")}</span><strong>{checklistLoaded && !checklistLoadError ? `${progress}%` : "—"}</strong><small>{checklistLoadError ? t("unableLoadChecklist") : checklistLoaded ? t("tasksComplete", { completed, total: tasks.length }) : t("loadingChecklist")}</small></article>
      <article><span><Heart size={16} /> {t("savedSuppliers")}</span><strong>{savedCount}</strong><small>{t("savedToAccount")}</small></article>
    </section>

    <section className="workspace-section-heading">
      <div><div className="eyebrow">{t("yourNextSteps")}</div><h2>{t("keepPlansMoving")}</h2></div>
      <button className="text-button" onClick={onOpenChecklist}>{t("openChecklist")} <ArrowRight size={14} /></button>
    </section>

    <div className="workspace-content-grid">
      <section className="workspace-panel">
        <div className="workspace-panel-heading"><span className="workspace-panel-icon"><ListChecks size={18} /></span><div><h3>{t("weddingChecklist")}</h3><p>{t("personalPlanningTasks")}</p></div></div>
        {checklistLoadError ? <p className="workspace-loading">{t("checklistLoadFailed")}</p>
          : !checklistLoaded ? <p className="workspace-loading">{t("loadingChecklist")}</p>
          : tasks.length ? <>
            <div className="workspace-progress"><div className="progress-track"><span style={{ width: `${progress}%` }} /></div><strong>{progress}%</strong></div>
            <div className="workspace-task-list">
              {outstandingTasks.length ? outstandingTasks.map((task) => <div className="workspace-task" key={task.id}>
                <span className="workspace-task-dot" /><span>{task.title}</span><small>{task.due}</small>
              </div>) : <p className="workspace-complete"><CheckCircle2 size={16} /> {t("allChecklistTasksDone")}</p>}
            </div>
          </> : <div className="workspace-empty"><p>{t("checklistReady")}</p><button className="secondary-button" onClick={onOpenChecklist}>{t("openChecklist")} <ArrowRight size={13} /></button></div>}
      </section>

      <section className="workspace-panel workspace-bookings-panel">
        <div className="workspace-panel-heading"><span className="workspace-panel-icon booking"><CalendarDays size={18} /></span><div><h3>{t("supplierRequests")}</h3><p>{t("supplierUpdates")}</p></div></div>
        {bookingsLoadError ? <p className="workspace-loading">{t("bookingLoadFailed")}</p>
          : bookings === null ? <p className="workspace-loading">{t("loadingRequests")}</p>
          : bookings.length ? <div className="workspace-booking-list">{bookings.slice(0, 3).map((booking) => <div className="workspace-booking" key={booking.id}>
            <span className="workspace-task-dot" /><span><strong>{booking.supplierName}</strong><small>{booking.categoryName || t(`status_${booking.status}`, { defaultValue: booking.status })}</small></span><span className="status-pill">{t(`status_${booking.status}`, { defaultValue: booking.status })}</span>
          </div>)}</div>
            : <div className="workspace-empty"><p>{t("noRequestsFindSupplier")}</p><button className="secondary-button" onClick={onDiscover}>{t("browseSuppliers")} <ArrowRight size={13} /></button></div>}
        {bookings?.length > 3 && <button className="text-button workspace-panel-link" onClick={onOpenBookings}>{t("viewAllRequests")} <ArrowRight size={14} /></button>}
      </section>
    </div>

    <section className="workspace-suppliers">
      <div className="workspace-section-heading">
        <div><div className="eyebrow">{t("discoverWeddingTeam")}</div><h2>{t("professionalsToStart")}</h2></div>
        <button className="text-button" onClick={onDiscover}>{t("browseAllSuppliers")} <ArrowRight size={14} /></button>
      </div>
      {directoryError
        ? <p className="workspace-loading" role="status">{t("directoryUnavailable")}</p>
        : !directoryLoaded
          ? <p className="workspace-loading">{t("loadingApprovedSuppliers")}</p>
          : suppliers.length
            ? <div className="supplier-grid">{suppliers.slice(0, 3).map((supplier) => <SupplierCard
              key={supplier.id}
              supplier={supplier}
              saved={savedSuppliers.includes(supplier.id)}
              onSave={onSaveSupplier}
              onOpen={onOpenSupplier}
            />)}</div>
            : <div className="workspace-empty"><p>{t("noApprovedSuppliers")}</p></div>}
    </section>

    <footer className="workspace-privacy"><ShieldCheck size={15} /> {t("planningDetailsPrivate")}</footer>
  </div>;
}

function WeddingDateCard({ weddingDate, onSave }) {
  const { t, i18n } = useTranslation();
  const [editing, setEditing] = useState(!weddingDate);
  const [date, setDate] = useState(weddingDate || "");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setDate(weddingDate || "");
    setEditing(!weddingDate);
  }, [weddingDate]);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      if (await onSave(date)) setEditing(false);
    } finally {
      setSaving(false);
    }
  }

  const days = weddingDate ? daysUntilDate(weddingDate) : null;
  const formattedDate = weddingDate
    ? new Intl.DateTimeFormat(i18n.language, { dateStyle: "long", timeZone: "UTC" })
      .format(new Date(`${weddingDate}T00:00:00Z`))
    : t("weddingDateNotSet");
  const countdown = days === null ? t("setWeddingDatePrompt")
    : days > 0 ? t("weddingDaysRemaining", { count: days })
      : days === 0 ? t("weddingDateToday")
        : t("weddingDaysPast", { count: Math.abs(days) });

  return <section className="wedding-date-card">
    <span className="wedding-date-icon"><CalendarDays size={20} /></span>
    <div className="wedding-date-summary">
      <span>{t("weddingDate")}</span>
      <strong>{formattedDate}</strong>
      <small>{countdown}</small>
    </div>
    {editing ? <form className="wedding-date-form" onSubmit={submit}>
      <label>
        <input type="date" aria-label={t("weddingDate")} value={date} onChange={(event) => setDate(event.target.value)} />
      </label>
      <button className="primary-button" type="submit" disabled={saving}>{saving ? t("saving") : t("saveWeddingDate")}</button>
      {weddingDate && <button className="secondary-button" type="button" onClick={() => { setDate(weddingDate); setEditing(false); }}>{t("cancel")}</button>}
    </form> : <button className="secondary-button wedding-date-edit" onClick={() => setEditing(true)}>{t("editWeddingDate")}</button>}
  </section>;
}

function SupplierCard({ supplier, saved, onSave, onOpen }) {
  const { t } = useTranslation();
  const tagKeys = {
    "A little magic": "supplierTagMagic",
    "Guest favourite": "supplierTagFavorite",
    "Date availability": "supplierTagAvailability",
    "Verified supplier": "verifiedSupplier",
  };
  const categoryLabel = supplierCategoryLabel(t, supplier.category);
  return <article className="supplier-card"><button className="supplier-image-button" onClick={() => onOpen(supplier)} aria-label={t("viewSupplier", { name: supplier.name })}><img src={imageUrl(supplier.image)} alt="" loading="lazy" /><span className="supplier-tag"><Sparkles size={12} />{t(tagKeys[supplier.tag] || "verifiedSupplier")}</span></button><button className={`save-button${saved ? " is-saved" : ""}`} onClick={() => onSave(supplier.id)} aria-label={saved ? t("removeSavedSupplier") : t("saveSupplier")}><Heart size={17} fill={saved ? "currentColor" : "none"} /></button><div className="supplier-info" onClick={() => onOpen(supplier)} role="button" tabIndex={0} onKeyDown={(event) => event.key === "Enter" && onOpen(supplier)}><div className="supplier-category">{categoryLabel} <span>·</span> {supplier.place}</div><h3>{supplier.name} {supplier.verified && <span className="verified"><Check size={11} /></span>}</h3><div className="supplier-meta"><span><Star size={14} fill="currentColor" /> {supplier.rating} <small>({supplier.reviews})</small></span><strong>{formatSupplierPrice(t, supplier)}</strong></div></div></article>;
}

function localizedCountryName(code, language, options = []) {
  const fallback = options.find((option) => option.code === code)?.name || code;
  try {
    return new Intl.DisplayNames([language], { type: "region" }).of(code) || fallback;
  } catch {
    return fallback;
  }
}

function CountryMultiSelect({
  label,
  countryOptions = [],
  countryLoadStatus = "loaded",
  onRetryCountries,
  value = [],
  onChange,
  initiallyOpen = false,
}) {
  const { t, i18n } = useTranslation();
  const [search, setSearch] = useState("");
  const detailsRef = useRef(null);
  useEffect(() => {
    if (initiallyOpen && detailsRef.current) detailsRef.current.open = true;
  }, [initiallyOpen]);
  const filteredOptions = countryOptions.filter((country) =>
    localizedCountryName(country.code, i18n.language, countryOptions).toLocaleLowerCase(i18n.language)
      .includes(search.toLocaleLowerCase(i18n.language)));
  function toggleCountry(code) {
    onChange(value.includes(code)
      ? value.filter((selected) => selected !== code)
      : [...value, code]);
  }
  const summaryText = countryLoadStatus === "loading"
    ? t("countryListLoading")
    : countryLoadStatus === "error"
      ? t("countryListUnavailable")
      : value.length
        ? t("selectedCountriesCount", { count: value.length })
        : t("chooseCountries");
  return <div className="country-multi-select">
    <span className="country-select-label">{label}</span>
    <details ref={detailsRef} className="country-select-details">
      <summary>
        <Globe size={16} aria-hidden="true" />
        <span>{summaryText}</span>
        <ChevronDown size={15} aria-hidden="true" />
      </summary>
      <div className="country-options-panel">
        <label className="country-search-field">
          <Search size={15} aria-hidden="true" />
          <input type="search" value={search} onChange={(event) => setSearch(event.target.value)}
            placeholder={t("searchCountries")} aria-label={t("searchCountries")}
            disabled={countryLoadStatus !== "loaded"} />
        </label>
        {countryLoadStatus === "loading" && <div className="country-options-message" role="status">
          <span className="country-loading-indicator" />{t("countryListLoading")}
        </div>}
        {countryLoadStatus === "error" && <div className="country-options-message country-options-error" role="alert">
          <span>{t("countryListUnavailable")}</span>
          <button type="button" className="text-button" onClick={onRetryCountries}>{t("retryCountryList")}</button>
        </div>}
        {countryLoadStatus === "loaded" && <div className="country-options-list">
          {filteredOptions.map((country) => <label key={country.code}>
            <input type="checkbox" checked={value.includes(country.code)} onChange={() => toggleCountry(country.code)} />
            <span className="country-code-badge">{country.code}</span>
            <span>{localizedCountryName(country.code, i18n.language, countryOptions)}</span>
          </label>)}
          {!filteredOptions.length && <span className="country-options-empty">{t("noCountriesMatch")}</span>}
        </div>}
      </div>
    </details>
      {value.length > 0 && <div className="country-selected-list" aria-label={t("selectedCountries")}>
        {value.map((code) => <button type="button" className="country-selected-chip" key={code}
          onClick={() => toggleCountry(code)} aria-label={t("removeCountry", {
            country: localizedCountryName(code, i18n.language, countryOptions),
          })}>
          <span className="country-code-badge">{code}</span>
          <span>{localizedCountryName(code, i18n.language, countryOptions)}</span>
          <X size={13} aria-hidden="true" />
        </button>)}
      </div>}
  </div>;
}

function CountryPreferences({
  countryOptions,
  countryLoadStatus,
  onRetryCountries,
  countryCodes,
  onSave,
}) {
  const { t } = useTranslation();
  const [selectedCountries, setSelectedCountries] = useState(countryCodes);
  const [saving, setSaving] = useState(false);
  useEffect(() => setSelectedCountries(countryCodes), [countryCodes]);
  async function save() {
    setSaving(true);
    try {
      await onSave(selectedCountries);
    } finally {
      setSaving(false);
    }
  }
  return <section className="country-preferences">
    <div><strong>{t("countryFilterTitle")}</strong><p>{t("countryFilterIntro")}</p></div>
    <CountryMultiSelect label={t("yourCountries")} countryOptions={countryOptions}
      countryLoadStatus={countryLoadStatus} onRetryCountries={onRetryCountries}
      value={selectedCountries} onChange={setSelectedCountries} />
    <button className="primary-button" disabled={saving || !selectedCountries.length || countryLoadStatus !== "loaded"} onClick={save}>{saving ? t("saving") : t("saveCountryPreferences")}</button>
  </section>;
}

function SupplierLocationMap({ value, interactive = false, label, onChange }) {
  const { t } = useTranslation();
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const onChangeRef = useRef(onChange);
  const valueRef = useRef(value);
  const [mapLoadError, setMapLoadError] = useState(false);
  const hasCoordinates = Number.isFinite(value?.latitude) && Number.isFinite(value?.longitude);
  valueRef.current = value;

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    if (!containerRef.current) return undefined;
    let active = true;
    let map;
    let resizeTimer;
    import("leaflet").then(({ default: Leaflet }) => {
      if (!active || !containerRef.current) return;
      const selectedLocation = valueRef.current;
      const selectedCoordinates = Number.isFinite(selectedLocation?.latitude) && Number.isFinite(selectedLocation?.longitude);
      map = Leaflet.map(containerRef.current, {
        center: [selectedCoordinates ? selectedLocation.latitude : 20, selectedCoordinates ? selectedLocation.longitude : 0],
        zoom: selectedCoordinates ? 13 : 2,
        scrollWheelZoom: interactive,
        dragging: interactive,
        doubleClickZoom: interactive,
        touchZoom: interactive,
        boxZoom: interactive,
        keyboard: interactive,
      });
      mapRef.current = map;
      Leaflet.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(map);

      function updatePin(latitude, longitude) {
        if (!interactive) return;
        if (!markerRef.current) {
          markerRef.current = Leaflet.marker([latitude, longitude], {
            draggable: true,
            icon: Leaflet.divIcon({ className: "supplier-map-pin", iconSize: [24, 34], iconAnchor: [12, 31] }),
          }).addTo(map);
          markerRef.current.on("dragend", () => {
            const point = markerRef.current.getLatLng();
            onChangeRef.current?.({ latitude: point.lat, longitude: point.lng });
          });
        } else {
          markerRef.current.setLatLng([latitude, longitude]);
        }
        onChangeRef.current?.({ latitude, longitude });
      }

      if (interactive) map.on("click", (event) => updatePin(event.latlng.lat, event.latlng.lng));
      if (selectedCoordinates) {
        markerRef.current = Leaflet.marker([selectedLocation.latitude, selectedLocation.longitude], {
          draggable: interactive,
          icon: Leaflet.divIcon({ className: "supplier-map-pin", iconSize: [24, 34], iconAnchor: [12, 31] }),
        }).addTo(map);
        if (interactive) markerRef.current.on("dragend", () => {
          const point = markerRef.current.getLatLng();
          onChangeRef.current?.({ latitude: point.lat, longitude: point.lng });
        });
      }
      resizeTimer = window.setTimeout(() => map.invalidateSize(), 0);
    }).catch(() => {
      if (active) setMapLoadError(true);
    });
    return () => {
      active = false;
      window.clearTimeout(resizeTimer);
      map?.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, [interactive]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    if (hasCoordinates) {
      if (markerRef.current) markerRef.current.setLatLng([value.latitude, value.longitude]);
      map.setView([value.latitude, value.longitude], Math.max(map.getZoom(), 13));
    } else if (markerRef.current) {
      map.removeLayer(markerRef.current);
      markerRef.current = null;
    }
  }, [hasCoordinates, interactive, value?.latitude, value?.longitude]);

  return <div
    ref={containerRef}
    className={`supplier-location-map${interactive ? " is-interactive" : ""}`}
    role="region"
    aria-label={label || t("supplierMapLocation")}
  >{mapLoadError && <span className="supplier-map-error" role="alert">{t("supplierMapFailed")}</span>}</div>;
}

function WeddingAssistant() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [retryMessages, setRetryMessages] = useState(null);
  const [consented, setConsented] = useState(false);
  const messagesRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    messagesRef.current?.scrollTo({ top: messagesRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, loading, open]);

  async function sendMessages(conversation) {
    if (!consented) return;
    setLoading(true);
    setError("");
    try {
      const { data } = await assistantApi.chat(conversation);
      setMessages((current) => [...current, { role: "assistant", content: data.reply }]);
      setRetryMessages(null);
    } catch (requestError) {
      setRetryMessages(conversation);
      const response = requestError.response;
      const code = response?.data?.code || response?.data?.properties?.code;
      const errorKeys = {
        "assistant.model_unavailable": "assistantModelUnavailable",
        "assistant.model_invalid_response": "assistantModelInvalidResponse",
        "assistant.model_timeout": "assistantRequestTimeout",
        "assistant.provider_auth": "assistantProviderAuth",
        "assistant.provider_credit": "assistantProviderCredit",
        "assistant.provider_rate_limit": "assistantProviderRateLimit",
        "assistant.local_model_unavailable": "assistantLocalModelUnavailable",
        "assistant.local_model_invalid_response": "assistantLocalModelInvalidResponse",
        "assistant.local_model_timeout": "assistantRequestTimeout",
      };
      const statusKeys = {
        401: "assistantSignInAgain",
        403: "assistantCoupleOnly",
        500: "assistantServerError",
        502: "assistantModelInvalidResponse",
        503: "assistantModelUnavailable",
        504: "assistantRequestTimeout",
      };
      const requestTimedOut = ["ECONNABORTED", "ETIMEDOUT"].includes(requestError.code);
      setError(errorKeys[code] || statusKeys[response?.status]
        || (requestTimedOut ? "assistantRequestTimeout"
          : !response ? "assistantNetworkError" : response.data?.detail || "assistantError"));
    } finally {
      setLoading(false);
    }
  }

  function submit(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content || !consented || loading || retryMessages) return;
    const conversation = [...messages, { role: "user", content }].slice(-12);
    setMessages(conversation);
    setDraft("");
    sendMessages(conversation);
  }

  function startSuggestion(prompt) {
    setDraft(prompt);
    window.setTimeout(() => inputRef.current?.focus(), 0);
  }

  return <div className="wedding-assistant">
    {open && <section className="assistant-window" role="dialog" aria-modal="false" aria-labelledby="assistant-title">
      <header className="assistant-header">
        <span className="assistant-avatar"><Sparkles size={17} /></span>
        <div><h2 id="assistant-title">{t("assistantTitle")}</h2><p>{t("assistantSubtitle")}</p></div>
        <button className="assistant-close" onClick={() => setOpen(false)} aria-label={t("closeDialog")}><X size={17} /></button>
      </header>
      <div className="assistant-messages" ref={messagesRef} aria-live="polite">
        {!messages.length && <div className="assistant-welcome">
          <span className="assistant-welcome-icon"><Bot size={22} /></span>
          <strong>{t("assistantWelcome")}</strong>
          <p>{t("assistantWelcomeDetail")}</p>
          {!consented && <button className="assistant-consent" onClick={() => setConsented(true)}>{t("assistantConsent")}</button>}
          {consented && <div className="assistant-suggestions">
            {[t("assistantSuggestionBudget"), t("assistantSuggestionSuppliers"), t("assistantSuggestionPriorities")].map((prompt) =>
              <button key={prompt} onClick={() => startSuggestion(prompt)}>{prompt}</button>)}
          </div>}
        </div>}
        {messages.map((message, index) => <div key={`${index}-${message.role}`} className={`assistant-message${message.role === "user" ? " own" : ""}`}>
          {message.role === "assistant" && <Sparkles size={13} />}
          <p>{message.content}</p>
        </div>)}
        {loading && <div className="assistant-message assistant-thinking"><Sparkles size={13} /><p>{t("assistantThinking")}</p></div>}
        {error && <div className="assistant-error" role="alert"><span>{t(error, { defaultValue: error })}</span><button disabled={loading} onClick={() => sendMessages(retryMessages)}><RotateCcw size={13} /> {t("assistantRetry")}</button></div>}
      </div>
      <form className="assistant-compose" onSubmit={submit}>
        <textarea
          ref={inputRef}
          className="wedding-assistant-input"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              event.currentTarget.form.requestSubmit();
            }
          }}
          placeholder={t("assistantPlaceholder")}
          aria-label={t("assistantPlaceholder")}
          maxLength={1200}
          rows={2}
          disabled={!consented || loading || Boolean(retryMessages)}
        />
        <button type="submit" className="assistant-send" aria-label={t("assistantSend")} disabled={!consented || loading || Boolean(retryMessages) || !draft.trim()}><Send size={16} /></button>
      </form>
      <p className="assistant-privacy">{t("assistantPrivacy")}</p>
    </section>}
    <button className={`assistant-launcher${open ? " is-open" : ""}`} onClick={() => setOpen((value) => !value)} aria-label={open ? t("closeDialog") : t("assistantOpen")} aria-expanded={open}>
      {open ? <X size={20} /> : <><Sparkles size={18} /><span>{t("assistantOpen")}</span></>}
    </button>
  </div>;
}

function CollaborationsWorkspace({ role, onNotice, onSelectedCollaborations }) {
  const { t } = useTranslation();
  const translationLanguages = [
    { code: "en", label: "English" },
    { code: "fr", label: "French" },
    { code: "el", label: "Greek" },
    { code: "ar", label: "Arabic" },
  ];
  const [collaborations, setCollaborations] = useState([]);
  const [profiles, setProfiles] = useState([]);
  const [directory, setDirectory] = useState([]);
  const [services, setServices] = useState([]);
  const [servicesLoading, setServicesLoading] = useState(true);
  const [creatorServiceIds, setCreatorServiceIds] = useState([]);
  const [selectedServices, setSelectedServices] = useState({});
  const [inviteSuppliers, setInviteSuppliers] = useState({});
  const [nameTranslations, setNameTranslations] = useState({ en: "", fr: "", el: "", ar: "" });
  const [selectedInviteIds, setSelectedInviteIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadCollaborations() {
    try {
      const { data } = await (role === "CUSTOMER" ? collaborationsApi.listPublic() : collaborationsApi.list());
      setCollaborations(data);
      if (role === "CUSTOMER") onSelectedCollaborations(data.filter((collaboration) => collaboration.selected));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorLoadCollaborations"));
    }
  }

  useEffect(() => {
    let active = true;
    setLoading(true);
    if (role === "CUSTOMER") {
      collaborationsApi.listPublic().then(({ data }) => {
        if (active) {
          setCollaborations(data);
          onSelectedCollaborations(data.filter((collaboration) => collaboration.selected));
        }
      }).catch((error) => {
        if (active) onNotice(error.response?.data?.detail || t("errorLoadCollaborations"));
      }).finally(() => {
        if (active) setLoading(false);
      });
      return () => { active = false; };
    }
    Promise.allSettled([collaborationsApi.list(), suppliersApi.owned(), suppliersApi.list()]).then((results) => {
      if (!active) return;
      const [collaborationResult, profileResult, directoryResult] = results;
      if (collaborationResult.status === "fulfilled") setCollaborations(collaborationResult.value.data);
      else onNotice(collaborationResult.reason.response?.data?.detail || t("errorLoadCollaborations"));
      if (profileResult.status === "fulfilled") {
        const availableProfiles = profileResult.value.data.filter((profile) =>
          profile.approved && profile.active !== false);
        setProfiles(profileResult.value.data);
        setServicesLoading(true);
        Promise.all(availableProfiles.map(async (profile) => {
          const { data } = await supplierServicesApi.listOwned(profile.id);
          return data.map((service) => ({
            ...service,
            supplierId: profile.id,
            supplierName: profile.businessName,
          }));
        })).then((serviceGroups) => {
          if (active) setServices(serviceGroups.flat());
        }).catch((error) => {
          if (active) onNotice(error.response?.data?.detail || t("errorLoadServices"));
        }).finally(() => {
          if (active) setServicesLoading(false);
        });
      } else {
        setServicesLoading(false);
        onNotice(profileResult.reason.response?.data?.detail || t("errorLoadBusinessProfile"));
      }
      if (directoryResult.status === "fulfilled") setDirectory(directoryResult.value.data);
      else onNotice(directoryResult.reason.response?.data?.detail || t("directoryOffline"));
      setLoading(false);
    });
    return () => { active = false; };
  }, [role, onNotice, onSelectedCollaborations, t]);

  const profileIds = profiles.map((profile) => profile.id);
  const eligibleProfiles = profiles.filter((profile) => profile.approved && profile.active !== false);
  const inviteCandidates = directory.filter((supplier) => !profileIds.includes(supplier.id));
  const creatorSupplierId = services.find((service) => creatorServiceIds.includes(service.id))?.supplierId;

  async function createCollaboration(event) {
    event.preventDefault();
    if (!creatorSupplierId || translationLanguages.some(({ code }) => !nameTranslations[code].trim())
        || !selectedInviteIds.length || !creatorServiceIds.length) return;
    setSaving(true);
    try {
      await collaborationsApi.create({
        supplierId: Number(creatorSupplierId),
        names: Object.fromEntries(translationLanguages.map(({ code }) => [code, nameTranslations[code].trim()])),
        invitedSupplierIds: selectedInviteIds.map(Number),
        serviceIds: creatorServiceIds,
      });
      setNameTranslations({ en: "", fr: "", el: "", ar: "" });
      setSelectedInviteIds([]);
      setCreatorServiceIds([]);
      await loadCollaborations();
      onNotice(t("collaborationCreated"));
    } catch (error) {
      const status = error.response?.status;
      onNotice(error.response?.data?.detail
        || (status ? t("errorCreateCollaborationStatus", { status }) : t("errorCreateCollaborationConnection")));
    } finally {
      setSaving(false);
    }
  }

  async function invite(collaborationId) {
    const supplierId = inviteSuppliers[collaborationId];
    if (!supplierId) return;
    setSaving(true);
    try {
      await collaborationsApi.invite(collaborationId, Number(supplierId));
      await loadCollaborations();
      onNotice(t("collaborationInviteSent"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorInviteSupplier"));
    } finally {
      setSaving(false);
    }
  }

  async function removeInvite(collaborationId, supplierId) {
    setSaving(true);
    try {
      await collaborationsApi.removeInvite(collaborationId, supplierId);
      await loadCollaborations();
      onNotice(t("collaborationInvitationRemoved"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorRemoveCollaborationInvite"));
    } finally {
      setSaving(false);
    }
  }

  async function respond(collaborationId, accepted) {
    setSaving(true);
    try {
      await collaborationsApi.respond(collaborationId, accepted);
      await loadCollaborations();
      onNotice(t(accepted ? "collaborationAccepted" : "collaborationDeclined"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorRespondCollaboration"));
    } finally {
      setSaving(false);
    }
  }

  async function addServices(collaborationId) {
    const serviceIds = selectedServices[collaborationId] || [];
    if (!serviceIds.length) return;
    setSaving(true);
    try {
      await collaborationsApi.addItems(collaborationId, serviceIds.map(Number));
      setSelectedServices((current) => ({ ...current, [collaborationId]: [] }));
      await loadCollaborations();
      onNotice(t("collaborationServiceAdded"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorAddCollaborationService"));
    } finally {
      setSaving(false);
    }
  }

  function toggleCreatorService(service, checked) {
    setCreatorServiceIds((current) => {
      if (!checked) return current.filter((id) => id !== service.id);
      const currentSupplier = services.find((item) => current.includes(item.id))?.supplierId;
      if (currentSupplier && currentSupplier !== service.supplierId) return current;
      return [...current, service.id];
    });
  }

  async function activate(collaborationId) {
    setSaving(true);
    try {
      await collaborationsApi.activate(collaborationId);
      await loadCollaborations();
      onNotice(t("collaborationActivated"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorActivateCollaboration"));
    } finally {
      setSaving(false);
    }
  }

  async function toggleSelection(collaboration) {
    setSaving(true);
    try {
      if (collaboration.selected) await collaborationsApi.unselect(collaboration.id);
      else await collaborationsApi.select(collaboration.id);
      await loadCollaborations();
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorSelectCollaboration"));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="workspace-loading">{t("loadingCollaborations")}</p>;
  if (role === "SUPPLIER" && !profiles.length) {
    return <EmptyState title={t("collaborationNoProfileTitle")} detail={t("collaborationNoProfileDetail")} />;
  }

  return <section className="collaborations-workspace">
    {role === "SUPPLIER" && <form className="collaboration-create-form" onSubmit={createCollaboration}>
      <div><h2>{t("createCollaboration")}</h2><p>{t("createCollaborationIntro")}</p></div>
      {translationLanguages.map(({ code, label }) => <label key={code}>{t("collaborationName")} · {t(`language_${code}`)}
        <input value={nameTranslations[code]} onChange={(event) => setNameTranslations((current) => ({ ...current, [code]: event.target.value }))} maxLength={150} required placeholder={t("collaborationNamePlaceholder", { language: label })} />
      </label>)}
      <fieldset className="collaboration-invite-picker">
        <legend>{t("chooseYourServices")}</legend>
        <p className="collaboration-readiness">{t("chooseOptionsFromOneProfile")}</p>
        {services.map((service) => {
          const selectedSupplierId = services.find((item) => creatorServiceIds.includes(item.id))?.supplierId;
          return <label key={service.id}>
            <input type="checkbox" checked={creatorServiceIds.includes(service.id)}
              disabled={Boolean(selectedSupplierId && selectedSupplierId !== service.supplierId)}
              onChange={(event) => toggleCreatorService(service, event.target.checked)} />
            <span>{service.name} · {service.supplierName}{service.price != null ? ` · ${service.currency} ${service.price}` : ""}</span>
          </label>;
        })}
        {!services.length && <p className="collaboration-readiness">{t(servicesLoading ? "loadingServices" : "noServicesForCollaboration")}</p>}
      </fieldset>
      <fieldset className="collaboration-invite-picker"><legend>{t("chooseSuppliersToInvite")}</legend>
        {inviteCandidates.map((supplier) => <label key={supplier.id}>
          <input type="checkbox" checked={selectedInviteIds.includes(supplier.id)} onChange={(event) => setSelectedInviteIds((current) => event.target.checked
            ? [...current, supplier.id] : current.filter((id) => id !== supplier.id))} />
          <span>{supplier.businessName}</span>
        </label>)}
      </fieldset>
      <button className="primary-button" type="submit" disabled={saving || !eligibleProfiles.length || !creatorServiceIds.length || !selectedInviteIds.length}>{t("createCollaboration")}</button>
    </form>}

    {role === "CUSTOMER" && <div className="collaboration-intro"><h2>{t("coupleCollaborationsTitle")}</h2><p>{t("coupleCollaborationsIntro")}</p></div>}
    {collaborations.length ? <div className="collaboration-list">
      {collaborations.map((collaboration) => {
        const ownMembers = collaboration.members.filter((member) => profileIds.includes(member.supplierId));
        const pending = ownMembers.some((member) => member.status === "PENDING");
        const isCreator = profileIds.includes(collaboration.creatorSupplierId);
        const acceptedOwnedSupplierIds = ownMembers
          .filter((member) => member.status === "ACCEPTED")
          .map((member) => member.supplierId);
        const availableServices = services.filter((service) =>
          acceptedOwnedSupplierIds.includes(service.supplierId)
          && !collaboration.items.some((item) => item.serviceId === service.id));
        const candidates = inviteCandidates.filter((supplier) =>
          !collaboration.members.some((member) => member.supplierId === supplier.id));
        return <article className="collaboration-card" key={collaboration.id}>
          <div className="collaboration-card-heading">
            <div><span className="eyebrow">{t("collaborationSection")}</span><h2>{collaboration.name}</h2></div>
            {role === "SUPPLIER" && pending && <span className="collaboration-status pending">{t("collaborationInvitation")}</span>}
            {role === "SUPPLIER" && collaboration.active && <span className="collaboration-status">{t("collaborationActive")}</span>}
          </div>
          <div className="collaboration-members">
            {collaboration.members.map((member) => <span key={member.supplierId}>
              {member.supplierName}
              {role === "SUPPLIER" && member.status !== "ACCEPTED" && ` · ${t(member.status === "PENDING" ? "collaborationPending" : "collaborationDeclinedStatus")}`}
              {role === "SUPPLIER" && isCreator && !collaboration.active && member.status !== "ACCEPTED"
                && <button type="button" className="collaboration-remove-invite" disabled={saving}
                  aria-label={t("removeCollaborationInvite", { name: member.supplierName })}
                  onClick={() => removeInvite(collaboration.id, member.supplierId)}><X size={12} /></button>}
            </span>)}
          </div>
          {role === "SUPPLIER" && pending && <div className="collaboration-actions">
            <button type="button" className="primary-button" disabled={saving} onClick={() => respond(collaboration.id, true)}>{t("acceptInvitation")}</button>
            <button type="button" className="secondary-button" disabled={saving} onClick={() => respond(collaboration.id, false)}>{t("declineInvitation")}</button>
          </div>}
          <div className="collaboration-items">
            {collaboration.items.map((item) => <div className="collaboration-item" key={item.id}>
              {item.imageUrl && <img src={resolveMediaUrl(item.imageUrl)} alt="" />}
              <div><strong>{item.serviceName}</strong><small>{item.supplierName}</small>{item.description && <p>{item.description}</p>}</div>
              {item.price != null && <span className="collaboration-price">{item.currency} {item.price}</span>}
            </div>)}
            {!collaboration.items.length && <p className="collaboration-empty-items">{t("noCollaborationServices")}</p>}
          </div>
          {role === "SUPPLIER" && availableServices.length > 0 && !collaboration.active && <div className="collaboration-actions collaboration-manage">
            <fieldset className="collaboration-invite-picker">
              <legend>{t("chooseServicesForCollaboration")}</legend>
              {availableServices.map((service) => <label key={service.id}>
                <input type="checkbox"
                  checked={(selectedServices[collaboration.id] || []).includes(String(service.id))}
                  onChange={(event) => setSelectedServices((current) => {
                    const selected = current[collaboration.id] || [];
                    return { ...current, [collaboration.id]: event.target.checked
                      ? [...selected, String(service.id)]
                      : selected.filter((id) => id !== String(service.id)) };
                  })} />
                <span>{service.name} · {service.categoryName || service.supplierName}{service.price != null ? ` · ${service.currency} ${service.price}` : ""}</span>
              </label>)}
            </fieldset>
            <button type="button" className="secondary-button" disabled={saving || !(selectedServices[collaboration.id] || []).length} onClick={() => addServices(collaboration.id)}>{t("addCollaborationServices")}</button>
          </div>}
          {role === "SUPPLIER" && isCreator && !collaboration.active && <div className="collaboration-actions collaboration-manage">
            <label>{t("inviteSupplier")}<select value={inviteSuppliers[collaboration.id] || ""} onChange={(event) => setInviteSuppliers((current) => ({ ...current, [collaboration.id]: event.target.value }))}>
              <option value="">{t("chooseSupplierToInvite")}</option>
              {candidates.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.businessName}</option>)}
            </select></label>
            <button type="button" className="secondary-button" disabled={saving || !inviteSuppliers[collaboration.id]} onClick={() => invite(collaboration.id)}>{t("sendCollaborationInvite")}</button>
          </div>}
          {role === "SUPPLIER" && isCreator && !collaboration.active && <div className="collaboration-actions collaboration-manage">
            <span className="collaboration-readiness">{collaboration.readyToActivate ? t("collaborationReady") : t("collaborationWaiting")}</span>
            <button type="button" className="primary-button" disabled={saving || !collaboration.readyToActivate} onClick={() => activate(collaboration.id)}>{t("activateCollaboration")}</button>
          </div>}
          {role === "CUSTOMER" && <div className="collaboration-actions collaboration-manage">
            <button type="button" className={collaboration.selected ? "secondary-button" : "primary-button"} disabled={saving} onClick={() => toggleSelection(collaboration)}>
              {collaboration.selected ? t("collaborationSelected") : t("selectCollaboration")}
            </button>
          </div>}
        </article>;
      })}
    </div> : <EmptyState
      title={t(role === "SUPPLIER" ? "noSupplierCollaborations" : "noCoupleCollaborations")}
      detail={t(role === "SUPPLIER" ? "noSupplierCollaborationsDetail" : "noCoupleCollaborationsDetail")}
    />}
  </section>;
}

function SupplierDashboard({ countryOptions, countryLoadStatus, onRetryCountries, categories, onNotice, bookings }) {
  const { t, i18n } = useTranslation();
  const translationLanguages = [
    { code: "en", label: "English" },
    { code: "fr", label: "French" },
    { code: "el", label: "Greek" },
    { code: "ar", label: "Arabic" },
  ];
  const emptyServiceTranslations = () => Object.fromEntries(
    translationLanguages.map(({ code }) => [code, { name: "", description: "" }]),
  );
  const [profiles, setProfiles] = useState([]);
  const [profileId, setProfileId] = useState("");
  const [form, setForm] = useState(null);
  const [profileLanguage, setProfileLanguage] = useState("en");
  const [analytics, setAnalytics] = useState(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [mainImageUploading, setMainImageUploading] = useState(false);
  const [serviceOffers, setServiceOffers] = useState([]);
  const [serviceBookings, setServiceBookings] = useState([]);
  const [serviceBookingsLoading, setServiceBookingsLoading] = useState(false);
  const [serviceBookingsError, setServiceBookingsError] = useState(false);
  const [serviceBookingsReload, setServiceBookingsReload] = useState(0);
  const [serviceLoading, setServiceLoading] = useState(false);
  const [serviceSaving, setServiceSaving] = useState(false);
  const [editingServiceId, setEditingServiceId] = useState(null);
  const [serviceTranslations, setServiceTranslations] = useState(emptyServiceTranslations);
  const [servicePrice, setServicePrice] = useState("");
  const [servicePricingType, setServicePricingType] = useState("");
  const [serviceMinimumAmount, setServiceMinimumAmount] = useState(1);
  const [serviceMaximumAmount, setServiceMaximumAmount] = useState(1);
  const [serviceCategoryId, setServiceCategoryId] = useState("");
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
  const [categoriesSaving, setCategoriesSaving] = useState(false);
  const [availabilityServiceId, setAvailabilityServiceId] = useState(null);
  const [availabilityDates, setAvailabilityDates] = useState([]);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [availabilitySaving, setAvailabilitySaving] = useState(false);
  const [availabilityError, setAvailabilityError] = useState(false);
  const [availabilityReload, setAvailabilityReload] = useState(0);
  const [availabilityMonth, setAvailabilityMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const currentProfileId = useRef(profileId);
  currentProfileId.current = profileId;

  useEffect(() => {
    suppliersApi.owned().then(({ data }) => {
      setProfiles(data);
      if (data.length) setProfileId(String(data[0].id));
    }).catch((error) => onNotice(error.response?.data?.detail || t("errorLoadBusinessProfile")));
  }, [onNotice, t]);
  useEffect(() => {
    const profile = profiles.find((item) => String(item.id) === profileId);
    if (profile) setSelectedCategoryIds((profile.categories || []).map((category) => category.id));
    if (profile) setForm({
      businessName: profile.businessName,
      location: profile.location,
      description: profile.description || "",
      services: profile.services || "",
      availability: profile.availability || "",
      localizedContent: profile.localizedContent || {},
      countryCodes: profile.countryCodes || [],
      latitude: profile.latitude ?? null,
      longitude: profile.longitude ?? null,
      startingPrice: profile.startingPrice || "",
      currency: profile.currency || "USD",
      timeZone: profile.timeZone || "",
      contactEmail: profile.contactEmail || "",
      contactPhone: profile.contactPhone || "",
      whatsapp: profile.whatsapp || "",
      website: profile.website || "",
      facebook: profile.facebook || "",
      instagram: profile.instagram || "",
      tiktok: profile.tiktok || "",
    });
  }, [profileId, profiles]);
  useEffect(() => {
    if (!profileId) return;
    suppliersApi.analytics(profileId).then(({ data }) => setAnalytics(data))
      .catch((error) => onNotice(error.response?.data?.detail || t("errorLoadSupplierAnalytics")));
  }, [profileId, onNotice, t]);
  useEffect(() => {
    if (!profileId) return;
    let active = true;
    setAvailabilityServiceId(null);
    setAvailabilityDates([]);
    setServiceOffers([]);
    setServiceLoading(true);
    supplierServicesApi.listOwned(profileId).then(({ data }) => {
      if (active) setServiceOffers(data);
    }).catch((error) => {
      if (active) onNotice(error.response?.data?.detail || t("errorLoadServices"));
    }).finally(() => {
      if (active) setServiceLoading(false);
    });
    return () => { active = false; };
  }, [profileId, onNotice, t]);
  useEffect(() => {
    if (!profileId) return;
    let active = true;
    setServiceBookings([]);
    setServiceBookingsLoading(true);
    setServiceBookingsError(false);
    supplierServicesApi.ownedBookings(profileId).then(({ data }) => {
      if (active) setServiceBookings(data);
    }).catch((error) => {
      if (active) {
        setServiceBookingsError(true);
        onNotice(error.response?.data?.detail || t("errorLoadServiceBookings"));
      }
    }).finally(() => {
      if (active) setServiceBookingsLoading(false);
    });
    return () => { active = false; };
  }, [profileId, serviceBookingsReload, bookings, onNotice, t]);
  useEffect(() => {
    if (!profileId || !availabilityServiceId) {
      setAvailabilityLoading(false);
      return;
    }
    let active = true;
    setAvailabilityLoading(true);
    setAvailabilityError(false);
    const from = localDateString(new Date());
    const toDate = new Date();
    toDate.setFullYear(toDate.getFullYear() + 1);
    supplierServicesApi.ownedAvailability(profileId, availabilityServiceId, from, localDateString(toDate))
      .then(({ data }) => { if (active) setAvailabilityDates(data); })
      .catch((error) => {
        if (active) {
          setAvailabilityError(true);
          setAvailabilityDates([]);
          onNotice(error.response?.data?.detail || t("errorLoadAvailability"));
        }
      }).finally(() => { if (active) setAvailabilityLoading(false); });
    return () => { active = false; };
  }, [profileId, availabilityServiceId, availabilityReload, onNotice, t]);
  useEffect(() => {
    if (!editingServiceId) return;
    const service = serviceOffers.find((item) => item.id === editingServiceId);
    if (!service) return;
    const translations = emptyServiceTranslations();
    for (const { code } of translationLanguages) {
      translations[code] = {
        name: service.localizedContent?.[code]?.name || (code === "en" ? service.name : ""),
        description: service.localizedContent?.[code]?.description || (code === "en" ? service.description || "" : ""),
      };
    }
    setServiceTranslations(translations);
    setServicePrice(service.price ?? "");
    setServicePricingType(service.pricingType ?? "");
    setServiceMinimumAmount(service.minimumAmount ?? 1);
    setServiceMaximumAmount(service.maximumAmount ?? 1);
    setServiceCategoryId(String(service.categoryId || ""));
  }, [editingServiceId, serviceOffers]);

  async function saveProfile(event) {
    event.preventDefault();
    if (!form.countryCodes?.length) {
      onNotice(t("countriesRequired"));
      return;
    }
    setSaving(true);
    try {
      const { data } = await suppliersApi.update(profileId, {
        ...form,
        timeZone: form.timeZone || null,
        countryCodes: form.countryCodes,
        startingPrice: form.startingPrice === "" ? null : Number(form.startingPrice),
      });
      setProfiles((current) => current.map((item) => item.id === data.id ? data : item));
      onNotice(t("businessProfileSaved"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorSaveBusinessProfile"));
    } finally {
      setSaving(false);
    }
  }

  async function saveCategories() {
    setCategoriesSaving(true);
    try {
      const { data } = await suppliersApi.updateCategories(profileId, selectedCategoryIds);
      setProfiles((current) => current.map((profile) => profile.id === data.id ? data : profile));
      onNotice(t("supplierCategoriesSaved"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorSaveSupplierCategories"));
    } finally {
      setCategoriesSaving(false);
    }
  }

  async function uploadMedia(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const file = formElement.elements.namedItem("file")?.files?.[0];
    if (!file || !profileId) return;
    const formData = new FormData();
    formData.append("file", file);
    formData.append("caption", formElement.elements.namedItem("caption")?.value || "");
    setUploading(true);
    try {
      await suppliersApi.uploadMedia(profileId, formData);
      formElement.reset();
      onNotice(t("mediaAdded"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorUploadFile"));
    } finally {
      setUploading(false);
    }
  }

  async function uploadMainImage(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const file = formElement.elements.namedItem("mainImage")?.files?.[0];
    if (!file || !profileId) return;
    const formData = new FormData();
    formData.append("file", file);
    setMainImageUploading(true);
    try {
      const { data } = await suppliersApi.uploadMainImage(profileId, formData);
      setProfiles((current) => current.map((profile) => String(profile.id) === profileId
        ? { ...profile, mainImageUrl: data.url }
        : profile));
      formElement.reset();
      onNotice(t("mainImageSaved"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorUploadFile"));
    } finally {
      setMainImageUploading(false);
    }
  }

  function resetServiceForm() {
    setEditingServiceId(null);
    setServiceTranslations(emptyServiceTranslations());
    setServicePrice("");
    setServicePricingType("");
    setServiceMinimumAmount(1);
    setServiceMaximumAmount(1);
    setServiceCategoryId("");
  }

  function editService(service) {
    setEditingServiceId(service.id);
    const translations = emptyServiceTranslations();
    for (const { code } of translationLanguages) {
      translations[code] = {
        name: service.localizedContent?.[code]?.name || (code === "en" ? service.name : ""),
        description: service.localizedContent?.[code]?.description || (code === "en" ? service.description || "" : ""),
      };
    }
    setServiceTranslations(translations);
    setServicePrice(service.price ?? "");
    setServicePricingType(service.pricingType ?? "");
    setServiceMinimumAmount(service.minimumAmount ?? 1);
    setServiceMaximumAmount(service.maximumAmount ?? 1);
    setServiceCategoryId(String(service.categoryId || ""));
  }

  async function saveService(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const image = formElement.elements.namedItem("serviceImage")?.files?.[0];
    const localizedRequest = {
      translations: Object.fromEntries(translationLanguages.map(({ code }) => [code, {
        name: serviceTranslations[code].name.trim(),
        description: serviceTranslations[code].description.trim(),
      }])),
      price: servicePrice === "" ? null : Number(servicePrice),
      pricingType: servicePricingType || null,
      minimumAmount: Number(serviceMinimumAmount),
      maximumAmount: Number(serviceMaximumAmount),
      categoryId: Number(serviceCategoryId),
    };
    const formData = new FormData();
    formData.append("service", new Blob([JSON.stringify(localizedRequest)], { type: "application/json" }));
    if (image) formData.append("image", image);
    setServiceSaving(true);
    try {
      if (editingServiceId) {
        await supplierServicesApi.update(profileId, editingServiceId, formData);
      } else {
        await supplierServicesApi.create(profileId, formData);
      }
      const { data } = await supplierServicesApi.listOwned(profileId);
      setServiceOffers(data);
      formElement.reset();
      resetServiceForm();
      onNotice(t(editingServiceId ? "serviceUpdated" : "serviceAdded"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorSaveService"));
    } finally {
      setServiceSaving(false);
    }
  }

  async function deleteService(serviceId) {
    try {
      await supplierServicesApi.remove(profileId, serviceId);
      setServiceOffers((current) => current.filter((service) => service.id !== serviceId));
      if (editingServiceId === serviceId) resetServiceForm();
      onNotice(t("serviceDeleted"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorDeleteService"));
    }
  }

  function toggleServiceAvailability(serviceId) {
    setAvailabilityServiceId((current) => current === serviceId ? null : serviceId);
    setAvailabilityDates([]);
    setAvailabilityMonth(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  }

  async function changeAvailabilityDate(serviceId, date, shouldBlock) {
    setAvailabilitySaving(true);
    try {
      if (shouldBlock) await supplierServicesApi.blockDate(profileId, serviceId, date);
      else await supplierServicesApi.unblockDate(profileId, serviceId, date);
      if (currentProfileId.current !== profileId) return;
      setServiceBookings((current) => current.map((booking) => booking.serviceOfferingId === serviceId && booking.eventDate === date
        ? { ...booking, dateBlocked: shouldBlock } : booking));
      if (availabilityServiceId === serviceId) {
        setAvailabilityDates((current) => {
          const remaining = current.filter((item) => item.date !== date);
          return shouldBlock ? [...remaining, { date, status: "BLOCKED" }].sort((a, b) => a.date.localeCompare(b.date)) : remaining;
        });
      }
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorChangeAvailability"));
      if (currentProfileId.current === profileId) {
        setServiceBookingsReload((current) => current + 1);
        setAvailabilityReload((current) => current + 1);
      }
    } finally {
      setAvailabilitySaving(false);
    }
  }

  function profileTextValue(field) {
    return profileLanguage === "en"
      ? form[field]
      : form.localizedContent[profileLanguage]?.[field] || "";
  }

  function updateProfileText(field, value) {
    if (profileLanguage === "en") {
      setForm((current) => ({ ...current, [field]: value }));
      return;
    }
    setForm((current) => ({
      ...current,
      localizedContent: {
        ...current.localizedContent,
        [profileLanguage]: {
          ...current.localizedContent[profileLanguage],
          [field]: value,
        },
      },
    }));
  }

  if (!profiles.length) return <EmptyState title={t("profileUnderReview")} detail={t("profileNeedsApproval")} action={() => onNotice(t("applicationPending"))} />;
  if (!form) return <div className="payment-loading">{t("loadingSupplierProfile")}</div>;
  const selectedProfile = profiles.find((profile) => String(profile.id) === profileId);
  const supplierAvailabilityDayCount = new Date(
    availabilityMonth.getFullYear(), availabilityMonth.getMonth() + 1, 0,
  ).getDate();
  const supplierAvailabilityOffset = new Date(
    availabilityMonth.getFullYear(), availabilityMonth.getMonth(), 1,
  ).getDay();
  const supplierAvailabilityCells = Math.ceil((supplierAvailabilityOffset + supplierAvailabilityDayCount) / 7) * 7;
  const availabilityLimit = new Date();
  availabilityLimit.setFullYear(availabilityLimit.getFullYear() + 1);
  const availabilityToday = localDateString(new Date());
  const dashboardLocale = i18n.language === "el" ? "el-GR" : i18n.language === "fr" ? "fr-FR" : i18n.language === "ar" ? "ar" : "en-US";
  return <div className="supplier-dashboard">
    <nav className="supplier-dashboard-shortcuts" aria-label={t("supplierDashboardSections")}>
      <a className="secondary-button" href="#supplier-services">{t("manageServices")} <ArrowDown size={14} /></a>
    </nav>
    <div className="dashboard-metrics"><article><span>{t("profileVisits")}</span><strong>{analytics?.visits ?? "—"}</strong></article><article><span>{t("bookings")}</span><strong>{analytics?.bookings ?? "—"}</strong></article><article><span>{t("messages")}</span><strong>{analytics?.messages ?? "—"}</strong></article><article><span>{t("profileStatus")}</span><strong>{profiles.find((item) => String(item.id) === profileId)?.approved ? t("live") : t("inReview")}</strong></article></div>
    {profiles.length > 1 && <label className="profile-picker">{t("businessProfile")}<select value={profileId} onChange={(event) => setProfileId(event.target.value)}>{profiles.map((profile) => <option key={profile.id} value={profile.id}>{profile.businessName}</option>)}</select></label>}
    <section className="supplier-services-editor supplier-categories-editor">
      <div className="panel-heading"><div><h2>{t("supplierWorkCategories")}</h2><p>{t("supplierWorkCategoriesIntro")}</p></div></div>
      <fieldset className="collaboration-invite-picker">
        <legend>{t("chooseWorkCategories")}</legend>
        {categories.map((category) => <label key={category.id}>
          <input type="checkbox" checked={selectedCategoryIds.some((id) => Number(id) === category.id)}
            onChange={(event) => setSelectedCategoryIds((current) => event.target.checked
              ? [...current, category.id]
              : current.filter((id) => Number(id) !== category.id))} />
          <span>{category.name}</span>
        </label>)}
      </fieldset>
      {!categories.length && <p className="supplier-section-message">{t("noWorkCategoriesAvailable")}</p>}
      <button type="button" className="primary-button" disabled={categoriesSaving || !categories.length}
        onClick={saveCategories}>{categoriesSaving ? t("saving") : t("saveWorkCategories")}</button>
    </section>
    <form className="business-form" onSubmit={saveProfile}><div className="panel-heading"><div><h2>{t("editBusinessPage")}</h2><p>{t("businessProfileIntro")}</p></div><BriefcaseBusiness size={19} /></div>
      <label className="profile-translation-language">{t("editProfileLanguage")}<select value={profileLanguage} onChange={(event) => setProfileLanguage(event.target.value)}>{translationLanguages.map((item) => <option value={item.code} key={item.code}>{t(`language_${item.code}`)}</option>)}</select></label>
      <CountryMultiSelect label={t("countriesServed")} countryOptions={countryOptions}
        countryLoadStatus={countryLoadStatus} onRetryCountries={onRetryCountries}
        value={form.countryCodes} onChange={(countryCodes) => setForm((current) => ({ ...current, countryCodes }))} />
      <label>{t("supplierTimeZone")}<input type="text" list="supplier-time-zones" value={form.timeZone || ""} onChange={(event) => setForm((current) => ({ ...current, timeZone: event.target.value }))} placeholder="Europe/Paris" /><datalist id="supplier-time-zones">{(Intl.supportedValuesOf ? Intl.supportedValuesOf("timeZone") : []).map((zone) => <option key={zone} value={zone} />)}</datalist><small>{t("supplierTimeZoneHelp")}</small></label>
      <section className="supplier-location-editor">
        <div><strong>{t("pinBusinessLocation")}</strong><p>{t("pinBusinessLocationHelp")}</p></div>
        <SupplierLocationMap value={{ latitude: form.latitude, longitude: form.longitude }} interactive onChange={({ latitude, longitude }) => setForm((current) => ({ ...current, latitude, longitude }))} />
        {form.latitude != null && form.longitude != null && <small>{t("coordinatesSelected", { latitude: form.latitude.toFixed(5), longitude: form.longitude.toFixed(5) })}</small>}
      </section>
      <div className="business-form-grid">{[
        ["businessName", "businessName"], ["location", "location"], ["startingPrice", "startingPrice"],
        ["currency", "currency"],
      ].map(([field, label]) => <label key={field}>{t(label)}<input type={field === "startingPrice" ? "number" : "text"} min={field === "startingPrice" ? "0" : undefined} step={field === "startingPrice" ? "0.01" : undefined} value={["businessName", "location"].includes(field) ? profileTextValue(field) : form[field]} onChange={(event) => ["businessName", "location"].includes(field) ? updateProfileText(field, event.target.value) : setForm((current) => ({ ...current, [field]: event.target.value }))} /></label>)}
      <section className="supplier-contact-editor wide-field">
        <div><strong>{t("publicContactDetails")}</strong><p>{t("publicContactDetailsHelp")}</p></div>
        <div className="business-form-grid">{[
          ["contactEmail", "publicContactEmail", "email", 180],
          ["contactPhone", "contactPhone", "tel", 30],
          ["whatsapp", "whatsapp", "tel", 30],
          ["website", "website", "text", 180],
          ["facebook", "facebook", "text", 500],
          ["instagram", "instagram", "text", 500],
          ["tiktok", "tiktok", "text", 500],
        ].map(([field, label, type, maxLength]) => <label key={field}>{t(label)}<input type={type} maxLength={maxLength} value={form[field]} onChange={(event) => setForm((current) => ({ ...current, [field]: event.target.value }))} /></label>)}</div>
      </section>
      <label className="wide-field">{t("description")}<textarea rows="4" maxLength="4000" value={profileTextValue("description")} onChange={(event) => updateProfileText("description", event.target.value)} /></label>
      <label className="wide-field">{t("servicesAndPackages")}<textarea rows="3" maxLength="500" value={profileTextValue("services")} onChange={(event) => updateProfileText("services", event.target.value)} /></label>
      <label className="wide-field">{t("availability")}<textarea rows="3" maxLength="8000" placeholder={t("availabilityPlaceholder")} value={profileTextValue("availability")} onChange={(event) => updateProfileText("availability", event.target.value)} /></label></div>
      <button className="primary-button" disabled={saving}>{saving ? t("saving") : t("saveBusinessPage")} <Check size={14} /></button>
    </form>
    <form className="main-image-upload-form" onSubmit={uploadMainImage}>
      <div className="main-image-preview">
        {selectedProfile?.mainImageUrl
          ? <img src={resolveMediaUrl(selectedProfile.mainImageUrl)} alt={t("mainSupplierImage")} />
          : <span>{t("noMainImage")}</span>}
      </div>
      <div><strong>{t("mainSupplierImage")}</strong><p>{t("mainImageHelp")}</p><input name="mainImage" type="file" accept="image/jpeg,image/png,image/webp" required aria-label={t("chooseMainImage")} /></div>
      <button className="secondary-button" disabled={mainImageUploading}>{mainImageUploading ? t("uploading") : t("saveMainImage")}</button>
    </form>
    <section className="supplier-services-editor" id="supplier-services">
      <div className="panel-heading"><div><h2>{t("manageServices")}</h2><p>{t("manageServicesIntro")}</p></div></div>
      {serviceLoading ? <p className="supplier-section-message">{t("loadingServices")}</p>
        : serviceOffers.length > 0 && <div className="service-editor-list">{serviceOffers.map((service) => {
          const translated = service.localizedContent?.[profileLanguage];
          const displayName = translated?.name || service.name;
          const displayDescription = translated?.description || service.description;
          return <article className="service-editor-item" key={service.id}>
            {service.imageUrl && <img src={resolveMediaUrl(service.imageUrl)} alt={displayName || service.name} />}
            <div><strong>{displayName || service.name}</strong><p>{service.categoryName}{pricingTypeLabels[service.pricingType] ? ` · ${t(pricingTypeLabels[service.pricingType])}` : ""}</p><p>{t("serviceQuantityRange", { min: service.minimumAmount, max: service.maximumAmount })}</p><p>{displayDescription || service.description}</p></div>
            <span>{service.price != null ? `${service.currency} ${service.price}` : t("priceOnRequest")}</span>
            <button type="button" className="booking-action" onClick={() => editService(service)}>{t("edit")}</button>
            <button type="button" className="booking-action" onClick={() => deleteService(service.id)}>{t("delete")}</button>
            <button type="button" className="booking-action" disabled={availabilitySaving} onClick={() => toggleServiceAvailability(service.id)}>
              {availabilityServiceId === service.id ? t("hideAvailability") : t("manageAvailability")}
            </button>
            <section className="service-bookings" aria-label={`${t("serviceBookings")} - ${displayName}`}>
              <h4>{t("serviceBookings")}</h4>
              {serviceBookingsLoading ? <p>{t("loadingServiceBookings")}</p>
                : serviceBookingsError ? <div role="alert"><p>{t("errorLoadServiceBookings")}</p><button type="button" className="booking-action" onClick={() => setServiceBookingsReload((current) => current + 1)}>{t("invitationRetry")}</button></div>
                  : serviceBookings.filter((booking) => booking.serviceOfferingId === service.id).length === 0 ? <p>{t("noServiceBookings")}</p>
                    : <ul>{serviceBookings.filter((booking) => booking.serviceOfferingId === service.id).map((booking) => {
                      const warn = activeServiceBookingStatuses.has(booking.status) && !booking.dateBlocked;
                      return <li key={booking.id}>
                        <div className="service-booking-details">
                          <strong>{booking.customerName}</strong>
                          <span>{booking.eventDate} · {t(`status_${booking.status}`, { defaultValue: booking.status })}</span>
                        </div>
                        {warn && <div className="service-booking-warning">
                          <span>{t("serviceDateNotBlocked")}</span>
                          {booking.eventDate >= availabilityToday && <button type="button" className="booking-action" disabled={availabilitySaving || availabilityLoading}
                            onClick={() => changeAvailabilityDate(service.id, booking.eventDate, true)}>{t("blockDate")}</button>}
                        </div>}
                      </li>;
                    })}</ul>}
            </section>
            {availabilityServiceId === service.id && <div className="supplier-availability-manager">
              <div className="availability-calendar-heading">
                <button type="button" className="booking-action" aria-label={t("previousMonth")}
                  disabled={availabilityMonth.getFullYear() === new Date().getFullYear() && availabilityMonth.getMonth() <= new Date().getMonth()}
                  onClick={() => setAvailabilityMonth((month) => new Date(month.getFullYear(), month.getMonth() - 1, 1))}>‹</button>
                <strong>{availabilityMonth.toLocaleDateString(dashboardLocale, { month: "long", year: "numeric" })}</strong>
                <button type="button" className="booking-action" aria-label={t("nextMonth")}
                  disabled={new Date(availabilityMonth.getFullYear(), availabilityMonth.getMonth() + 1, 1) > availabilityLimit}
                  onClick={() => setAvailabilityMonth((month) => new Date(month.getFullYear(), month.getMonth() + 1, 1))}>›</button>
              </div>
              <div className="availability-calendar-grid supplier-availability-grid">
                {Array.from({ length: 7 }, (_, index) => <span className="availability-weekday" key={`supplier-weekday-${index}`}>{new Date(2024, 0, 7 + index).toLocaleDateString(dashboardLocale, { weekday: "short" })}</span>)}
                {Array.from({ length: supplierAvailabilityCells }, (_, index) => {
                  const day = index - supplierAvailabilityOffset + 1;
                  if (day < 1 || day > supplierAvailabilityDayCount) return <span className="availability-empty" key={`supplier-empty-${index}`} />;
                  const dayDate = localDateString(new Date(availabilityMonth.getFullYear(), availabilityMonth.getMonth(), day));
                  const status = availabilityDates.find((item) => item.date === dayDate)?.status;
                  const disabled = dayDate < availabilityToday || availabilityLoading || availabilitySaving || availabilityError;
                  return <button type="button" key={dayDate}
                    className={`availability-day${status === "BLOCKED" ? " unavailable" : ""}`}
                    disabled={disabled}
                    aria-label={`${dayDate}, ${t(status === "BLOCKED" ? "blocked" : "available")}`}
                    title={t(status === "BLOCKED" ? "clickToUnblock" : "clickToBlock")}
                    onClick={() => changeAvailabilityDate(service.id, dayDate, status !== "BLOCKED")}>{day}</button>;
                })}
              </div>
              {availabilityLoading && <p>{t("loadingAvailability")}</p>}
              {availabilityError && <p className="auth-error" role="alert">{t("supplierAvailabilityLoadError")} <button type="button" className="booking-action" onClick={() => setAvailabilityReload((current) => current + 1)}>{t("invitationRetry")}</button></p>}
              {!availabilityLoading && !availabilityError && <p>{t("supplierAvailabilityHelp")}</p>}
            </div>}
          </article>;
        })}</div>}
      <form className="service-editor-form" onSubmit={saveService}>
        <h3>{t(editingServiceId ? "editService" : "addService")}</h3>
        <p className="service-translations-help">{t("serviceTranslationsHelp")}</p>
        <div className="service-editor-fields">
          {translationLanguages.map(({ code }) => <fieldset className="service-translation-fields" key={code}>
            <legend>{t(`language_${code}`)}</legend>
            <label>{t("serviceName")}<input value={serviceTranslations[code].name} onChange={(event) => setServiceTranslations((current) => ({ ...current, [code]: { ...current[code], name: event.target.value } }))} maxLength="150" required /></label>
            <label>{t("serviceDescription")}<textarea rows="2" maxLength="2000" value={serviceTranslations[code].description} onChange={(event) => setServiceTranslations((current) => ({ ...current, [code]: { ...current[code], description: event.target.value } }))} /></label>
          </fieldset>)}
          <label>{t("serviceCategory")}<select value={serviceCategoryId}
            onChange={(event) => setServiceCategoryId(event.target.value)} required>
            <option value="">{t("chooseWorkCategory")}</option>
            {(selectedProfile?.categories || []).map((assignedCategory) => {
              const category = categories.find((item) => item.id === assignedCategory.id) || assignedCategory;
              return <option key={category.id} value={category.id}>{category.name}</option>;
            })}
          </select></label>
          <label>{t("price")}<input type="number" min="0" step="0.01" value={servicePrice} onChange={(event) => setServicePrice(event.target.value)} /></label>
          <ServicePricingField value={servicePricingType} onChange={setServicePricingType} />
          <label>{t("minimumQuantity")}<input type="number" min="1" max="2147483647" step="1" required value={serviceMinimumAmount} onChange={(event) => setServiceMinimumAmount(event.target.value)} /></label>
          <label>{t("maximumQuantity")}<input type="number" min={Math.max(1, Number(serviceMinimumAmount) || 1)} max="2147483647" step="1" required value={serviceMaximumAmount} onChange={(event) => setServiceMaximumAmount(event.target.value)} /></label>
          <label className="wide-field">{t("serviceImage")}<input name="serviceImage" type="file" accept="image/jpeg,image/png,image/webp" /></label>
        </div>
        <div className="service-editor-actions">
          <button className="primary-button" disabled={serviceSaving || !selectedProfile?.categories?.length}>{serviceSaving ? t("saving") : t(editingServiceId ? "saveService" : "addService")}</button>
          {editingServiceId && <button type="button" className="secondary-button" onClick={resetServiceForm}>{t("cancel")}</button>}
        </div>
      </form>
    </section>
    <form className="media-upload-form" onSubmit={uploadMedia}><div><strong>{t("addToGallery")}</strong><p>{t("supportedMediaTypes")}</p></div><input name="file" type="file" accept="image/jpeg,image/png,image/webp,video/mp4" required aria-label={t("chooseMediaFile")} /><input name="caption" maxLength="180" placeholder={t("optionalCaption")} /><button className="secondary-button" disabled={uploading}>{uploading ? t("uploading") : t("uploadMedia")}</button></form>
  </div>;
}

function GuestsWorkspace({ account, countryOptions, onNotice }) {
  const { t, i18n } = useTranslation();
  const phoneCountries = useMemo(() => {
    const listedCountries = countryOptions.filter(({ code }) => supportedPhoneCountries.includes(code));
    return listedCountries.length
      ? listedCountries
      : supportedPhoneCountries.map((code) => ({ code, name: code }));
  }, [countryOptions]);
  const supportedCountries = supportedPhoneCountries;
  const preferredCountry = (account.countryCodes || []).find((code) =>
    phoneCountries.some((country) => country.code === code))
    || (phoneCountries.some((country) => country.code === "US") ? "US" : phoneCountries[0]?.code)
    || "US";
  const [guests, setGuests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingGuest, setEditingGuest] = useState(null);
  const [phoneCountry, setPhoneCountry] = useState(preferredCountry);
  const [formData, setFormData] = useState({ name: "", personCount: 1, phone: "", approved: false });

  useEffect(() => {
    if (!phoneCountries.some((country) => country.code === phoneCountry)) {
      setPhoneCountry(preferredCountry);
    }
  }, [phoneCountries, phoneCountry, preferredCountry]);

  async function loadGuests() {
    setLoading(true);
    try {
      const { data } = await guestApi.list();
      setGuests(data);
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorLoadGuests"));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadGuests();
  }, [t, onNotice]);

  function openAddGuest() {
    setEditingGuest(null);
    setPhoneCountry(preferredCountry);
    setFormData({ name: "", personCount: 1, phone: "", approved: false });
    setModalOpen(true);
  }

  function openEditGuest(guest) {
    const parsedPhone = guest.phone ? parsePhoneNumberFromString(guest.phone) : undefined;
    const guestCountry = parsedPhone?.country && supportedCountries.includes(parsedPhone.country)
      ? parsedPhone.country
      : preferredCountry;
    setEditingGuest(guest);
    setPhoneCountry(guestCountry);
    setFormData({
      name: guest.name,
      personCount: guest.personCount,
      phone: parsedPhone?.nationalNumber || String(guest.phone || "").replace(/\D/g, ""),
      approved: guest.approved,
    });
    setModalOpen(true);
  }

  async function saveGuest(event) {
    event.preventDefault();
    if (!formData.name.trim()) {
      onNotice(t("guestNameRequired"));
      return;
    }
    const localPhone = formData.phone.trim();
    const parsedPhone = localPhone ? parsePhoneNumberFromString(localPhone, phoneCountry) : undefined;
    if (localPhone && (!parsedPhone || !parsedPhone.isPossible())) {
      onNotice(t("invalidGuestPhone"));
      return;
    }
    const guestData = { ...formData, phone: parsedPhone?.number || null };
    setSaving(true);
    try {
      if (editingGuest) {
        await guestApi.update(editingGuest.id, guestData);
        onNotice(t("guestUpdated"));
      } else {
        await guestApi.create(guestData);
        onNotice(t("guestAdded"));
      }
      setModalOpen(false);
      loadGuests();
    } catch (error) {
      onNotice(error.response?.data?.detail || (editingGuest ? t("errorUpdateGuest") : t("errorAddGuest")));
    } finally {
      setSaving(false);
    }
  }

  async function deleteGuest(guestId) {
    if (!window.confirm(t("confirmDeleteGuest"))) return;
    try {
      await guestApi.delete(guestId);
      setGuests((current) => current.filter((g) => g.id !== guestId));
      onNotice(t("guestDeleted"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorDeleteGuest"));
    }
  }

  const totalGuestCount = guests.reduce((total, guest) => total + (Number(guest.personCount) || 0), 0);
  const approvedGuestCount = guests.reduce(
    (total, guest) => total + (guest.approved ? Number(guest.personCount) || 0 : 0),
    0,
  );

  if (loading) return <p className="workspace-loading">{t("loadingGuests")}</p>;
  return <div className="guests-workspace"><div className="panel-heading"><div><h2>{t("guests")}</h2><p>{t("guestsIntro")}</p></div><Users size={20} /></div>
    <button className="primary-button" onClick={openAddGuest}><span>+</span> {t("addGuest")}</button>
    <div className="guest-count-summary">
      <article><span>{t("totalGuests")}</span><strong>{totalGuestCount}</strong></article>
      <article><span>{t("approvedGuests")}</span><strong>{approvedGuestCount} / {totalGuestCount}</strong></article>
    </div>
    {guests.length > 0 ? <div className="guest-list"><table><thead><tr><th>{t("guestName")}</th><th>{t("numberOfPeople")}</th><th>{t("phoneNumber")}</th><th>{t("approved")}</th><th>{t("guestActions")}</th></tr></thead><tbody>{guests.map((guest) => {
      const parsedPhone = guest.phone ? parsePhoneNumberFromString(guest.phone) : undefined;
      const phone = parsedPhone?.isPossible() ? parsedPhone.number.replace(/\D/g, "") : "";
      return <tr key={guest.id}><td>{guest.name}</td><td>{guest.personCount}</td><td>{guest.phone || "—"}</td><td>{guest.approved ? "✓" : "—"}</td><td className="guest-row-actions">
        {phone
          ? <a className="booking-action guest-whatsapp-link" href={`https://wa.me/${phone}`} target="_blank" rel="noreferrer" aria-label={t("openGuestWhatsApp", { name: guest.name })}><MessageCircle size={14} />{t("whatsapp")}</a>
          : <button className="booking-action guest-whatsapp-link" type="button" disabled title={t("guestPhoneRequired")}><MessageCircle size={14} />{t("whatsapp")}</button>}
        <button className="booking-action" onClick={() => openEditGuest(guest)}>{t("edit")}</button>
        <button className="booking-action" onClick={() => deleteGuest(guest.id)}>{t("delete")}</button>
      </td></tr>;
    })}</tbody></table></div> : <EmptyState title={t("noGuests")} detail={t("startAddingGuests")} action={openAddGuest} actionLabel={t("addGuest")} />}
    {modalOpen && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setModalOpen(false)}><section className="booking-modal" role="dialog" aria-modal="true"><button className="modal-close" onClick={() => setModalOpen(false)} aria-label={t("closeDialog")}><X size={19} /></button><h2>{editingGuest ? t("editGuest") : t("addGuest")}</h2>
      <form className="guest-editor-form" onSubmit={saveGuest}>
        <input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder={t("guestName")} required />
        <input type="number" value={formData.personCount} onChange={(e) => setFormData({ ...formData, personCount: e.target.value === "" ? "" : Number(e.target.value) })} placeholder={t("numberOfPeople")} min="1" required />
        <label className="guest-phone-label">{t("phoneNumber")}<div className="guest-phone-input">
          <select value={phoneCountry} onChange={(event) => setPhoneCountry(event.target.value)} aria-label={t("countryCallingCode")}>
            {phoneCountries.map((country) => <option value={country.code} key={country.code}>
              {localizedCountryName(country.code, i18n.language, countryOptions)} (+{getCountryCallingCode(country.code)})
            </option>)}
          </select>
          <input type="tel" autoComplete="tel-national" value={formData.phone}
            onChange={(event) => setFormData({ ...formData, phone: event.target.value })}
            placeholder={t("localPhoneNumber")} />
        </div></label>
        <label className="guest-approved-field"><input type="checkbox" checked={formData.approved} onChange={(e) => setFormData({ ...formData, approved: e.target.checked })} /> {t("approved")}</label>
        <button className="primary-button" disabled={saving}>{saving ? t("saving") : editingGuest ? t("saveChanges") : t("addGuest")}</button>
      </form>
    </section></div>}
  </div>;
}

const invitationTemplates = [
  { id: "garden", nameKey: "gardenTemplateName", descriptionKey: "gardenTemplateDescription", style: "garden" },
  { id: "editorial", nameKey: "editorialTemplateName", descriptionKey: "editorialTemplateDescription", style: "editorial" },
  { id: "classic", nameKey: "classicTemplateName", descriptionKey: "classicTemplateDescription", style: "classic" },
];

function InvitationsWorkspace({ account, onNotice, onOpenGuests }) {
  const { t, i18n } = useTranslation();
  const savedInvitation = useMemo(() => readSavedInvitation(account.id), [account.id]);
  const [invitation, setInvitation] = useState(() => savedInvitation || {
    templateId: "garden",
    coupleNames: account.name || "",
    eventDate: account.weddingDate || "",
    venue: "",
    message: "",
  });
  const [isSaved, setIsSaved] = useState(Boolean(savedInvitation));
  const [guests, setGuests] = useState([]);
  const [guestsLoading, setGuestsLoading] = useState(true);
  const [guestsFailed, setGuestsFailed] = useState(false);
  const [sendingGuestId, setSendingGuestId] = useState(null);

  async function loadGuests() {
    setGuestsLoading(true);
    setGuestsFailed(false);
    try {
      const { data } = await guestApi.list();
      setGuests(data);
    } catch (error) {
      setGuestsFailed(true);
      onNotice(error.response?.data?.detail || t("errorLoadGuests"));
    } finally {
      setGuestsLoading(false);
    }
  }

  useEffect(() => {
    loadGuests();
  }, [account.id]);

  const selectedTemplate = invitationTemplates.find(({ id }) => id === invitation.templateId) || invitationTemplates[0];
  const coupleNames = invitation.coupleNames.trim() || t("yourNames");
  const weddingDate = invitation.eventDate
    ? new Intl.DateTimeFormat(i18n.language, { dateStyle: "long", timeZone: "UTC" })
      .format(new Date(`${invitation.eventDate}T00:00:00Z`))
    : "";
  const invitationCopy = (guest) => [
    t("invitationWhatsAppGreeting", { guestName: guest.name }),
    invitation.message.trim() || t("defaultInvitationMessage"),
    t("invitationWhatsAppCoupleLine", { coupleNames }),
    weddingDate && `${t("weddingDate")}: ${weddingDate}`,
    invitation.venue.trim() && `${t("invitationVenue")}: ${invitation.venue.trim()}`,
  ].filter(Boolean).join("\n\n");

  function updateInvitation(field, value) {
    setInvitation((current) => ({ ...current, [field]: value }));
    setIsSaved(false);
  }

  function saveInvitation(event) {
    event.preventDefault();
    try {
      localStorage.setItem(`wedora-invitation-${account.id}`, JSON.stringify(invitation));
      setIsSaved(true);
      onNotice(t("invitationSaved"));
    } catch {
      onNotice(t("errorSaveInvitation"));
    }
  }

  async function sendInvitation(guest) {
    setSendingGuestId(guest.id);
    try {
      const message = invitation.message.trim() || t("defaultInvitationMessage");
      const venue = invitation.venue.trim();
      const image = createInvitationImage(
        invitation.templateId,
        coupleNames,
        message,
        weddingDate,
        venue,
        t("invitationCardEyebrow"),
        i18n.language,
      );
      const filename = `wedding-invitation-${guest.name.trim().replace(/[^\p{L}\p{N}-]+/gu, "-") || guest.id}.png`;
      const shareText = invitationCopy(guest);
      const formData = new FormData();
      formData.append("card", new File([image], filename, { type: "image/png" }));
      formData.append("message", shareText);
      await guestApi.sendInvitation(guest.id, formData);
      onNotice(t("invitationSent", { guestName: guest.name }));
    } catch (error) {
      if (error.name !== "AbortError") {
        onNotice(error.response?.data?.detail || t("errorSendInvitation"));
      }
    } finally {
      setSendingGuestId(null);
    }
  }

  return <div className="invitations-workspace">
    <section className="invitation-builder">
      <div className="invitation-builder-main">
        <div className="invitation-section-heading">
          <div><span className="invitation-section-kicker">{t("invitationStepOne")}</span><h2>{t("invitationTemplatesHeading")}</h2><p>{t("invitationTemplatesIntro")}</p></div>
          <Sparkles size={20} aria-hidden="true" />
        </div>
        <div className="invitation-template-grid">
          {invitationTemplates.map((template) => <button
            type="button"
            key={template.id}
            className={`invitation-template-option${invitation.templateId === template.id ? " selected" : ""}`}
            aria-pressed={invitation.templateId === template.id}
            onClick={() => updateInvitation("templateId", template.id)}>
            <InvitationCardPreview template={template} invitation={invitation} coupleNames={coupleNames} weddingDate={weddingDate} t={t} compact />
            <span className="invitation-template-info"><span><strong>{t(template.nameKey)}</strong><small>{t(template.descriptionKey)}</small></span><i>{invitation.templateId === template.id ? <CheckCircle2 size={18} /> : <ChevronRight size={17} />}</i></span>
          </button>)}
        </div>

        <form className="invitation-details-form" onSubmit={saveInvitation}>
          <div className="invitation-section-heading">
            <div><span className="invitation-section-kicker">{t("invitationStepTwo")}</span><h2>{t("invitationDetailsHeading")}</h2><p>{t("invitationDetailsIntro")}</p></div>
            <Pencil size={18} aria-hidden="true" />
          </div>
          <div className="invitation-fields">
            <label>{t("coupleNames")}
              <input value={invitation.coupleNames} onChange={(event) => updateInvitation("coupleNames", event.target.value)} placeholder={t("coupleNamesPlaceholder")} maxLength={100} required />
            </label>
            <label>{t("weddingDate")}
              <input type="date" value={invitation.eventDate} onChange={(event) => updateInvitation("eventDate", event.target.value)} />
            </label>
            <label className="invitation-wide-field">{t("invitationVenue")}
              <input value={invitation.venue} onChange={(event) => updateInvitation("venue", event.target.value)} placeholder={t("invitationVenuePlaceholder")} maxLength={150} />
            </label>
            <label className="invitation-wide-field">{t("invitationMessage")}
              <textarea value={invitation.message} onChange={(event) => updateInvitation("message", event.target.value)} placeholder={t("invitationMessagePlaceholder")} rows="3" maxLength={500} />
            </label>
          </div>
          <button className="primary-button invitation-save-button" type="submit"><Check size={16} />{isSaved ? t("invitationSavedLabel") : t("saveInvitation")}</button>
        </form>
      </div>

      <aside className="invitation-preview-panel">
        <div className="invitation-preview-heading"><div><span className="invitation-section-kicker">{t("invitationLivePreview")}</span><h2>{t("invitationPreview")}</h2></div><span className="invitation-preview-dot" /></div>
        <InvitationCardPreview template={selectedTemplate} invitation={invitation} coupleNames={coupleNames} weddingDate={weddingDate} t={t} />
        {!isSaved && <p className="invitation-unsaved-note">{t("invitationUnsaved")}</p>}
      </aside>
    </section>

    <section className="invitation-send-panel">
      <div className="invitation-send-heading"><div><span className="invitation-section-kicker">{t("invitationStepThree")}</span><h2>{t("sendInvitationsHeading")}</h2><p>{t("sendInvitationsIntro")}</p></div><div className="invitation-guest-total"><Users size={17} />{guests.length}</div></div>
      {guestsLoading ? <p className="workspace-loading">{t("loadingGuests")}</p> : guestsFailed
        ? <div className="invitation-guests-empty"><p>{t("invitationGuestsFailed")}</p><button className="secondary-button" onClick={loadGuests}>{t("invitationRetry")}</button></div>
        : guests.length ? <div className="invitation-guest-list">{guests.map((guest) => {
          const phone = String(guest.phone || "").replace(/[^\d]/g, "");
          return <div className="invitation-guest-row" key={guest.id}>
            <span className="invitation-guest-avatar">{guest.name.slice(0, 1).toUpperCase()}</span>
            <span className="invitation-guest-meta"><strong>{guest.name}</strong><small>{guest.phone || t("guestNoPhone")}</small></span>
            <button className={`invitation-send-button${!guest.phone || !isSaved || sendingGuestId !== null ? " disabled" : ""}`}
              type="button" disabled={!guest.phone || !isSaved || sendingGuestId !== null}
              title={!guest.phone ? t("guestNoPhone") : !isSaved ? t("saveInvitationFirst") : undefined}
              onClick={() => sendInvitation(guest)}>
              <Send size={14} />{sendingGuestId === guest.id ? t("preparingInvitation") : t("sendViaWhatsApp")}
            </button>
          </div>;
        })}</div>
        : <div className="invitation-guests-empty"><Users size={22} /><p>{t("noGuestsToInvite")}</p><button className="secondary-button" onClick={onOpenGuests}>{t("addGuests")}</button></div>}
    </section>
  </div>;
}

function InvitationCardPreview({ template, invitation, coupleNames, weddingDate, t, compact = false }) {
  return <div className={`invitation-card-preview invitation-style-${template.style}${compact ? " compact" : ""}`}>
    <div className="invitation-card-ornament" aria-hidden="true"><Flower2 size={compact ? 17 : 23} strokeWidth={1.2} /></div>
    <span className="invitation-card-eyebrow">{t("invitationCardEyebrow")}</span>
    <strong className="invitation-card-names">{coupleNames}</strong>
    <span className="invitation-card-message">{invitation.message.trim() || t("defaultInvitationMessage")}</span>
    <span className="invitation-card-date">{weddingDate || t("invitationDatePlaceholder")}</span>
    {invitation.venue.trim() && <span className="invitation-card-venue">{invitation.venue.trim()}</span>}
    <span className="invitation-card-flourish" aria-hidden="true">✦</span>
  </div>;
}

function SuppliersAdminPanel({ onNotice }) {
  const { t } = useTranslation();
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("all");
  const [toggling, setToggling] = useState({});

  const filterOptions = [
    { key: "all", label: t("filterAllSuppliers") },
    { key: "approved", label: t("filterApproved") },
    { key: "pending", label: t("filterPending") },
    { key: "active", label: t("filterActive") },
    { key: "inactive", label: t("filterInactive") },
  ];

  async function loadSuppliers() {
    setLoading(true);
    try {
      const { data } = await adminApi.suppliers();
      setSuppliers(data);
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorLoadSuppliers"));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadSuppliers();
  }, [t, onNotice]);

  function filteredSuppliers() {
    return suppliers.filter((s) => {
      if (filter === "approved") return s.status === "Approved";
      if (filter === "pending") return s.status === "Pending";
      if (filter === "active") return s.active;
      if (filter === "inactive") return !s.active;
      return true;
    });
  }

  async function toggleSupplierActive(supplierId, currentActive) {
    const newActive = !currentActive;
    setToggling((prev) => ({ ...prev, [supplierId]: true }));
    try {
      await adminApi.toggleSupplierActive(supplierId, newActive);
      setSuppliers((current) => current.map((s) => s.id === supplierId ? { ...s, active: newActive } : s));
      onNotice(newActive ? t("supplierActivated") : t("supplierDeactivated"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorToggleSupplier"));
    } finally {
      setToggling((prev) => ({ ...prev, [supplierId]: false }));
    }
  }

  if (loading) return <p className="workspace-loading">{t("loadingSuppliers")}</p>;
  const shown = filteredSuppliers();
  return <section className="admin-panel"><div className="panel-heading"><div><h2>{t("suppliers")}</h2><p>{t("suppliersManagement")}</p></div><BriefcaseBusiness size={19} /></div>
    <div className="filter-buttons">{filterOptions.map((opt) => <button key={opt.key} className={filter === opt.key ? "active" : ""} onClick={() => setFilter(opt.key)}>{opt.label}</button>)}</div>
    {shown.length > 0 ? <div className="supplier-list"><table><thead><tr><th>{t("businessName")}</th><th>{t("location")}</th><th>{t("status")}</th><th>{t("active")}</th><th>{t("categories")}</th><th>{t("rating")}</th></tr></thead><tbody>{shown.map((supplier) => <tr key={supplier.id}><td><strong>{supplier.businessName}</strong></td><td>{supplier.location}</td><td><span className="status-pill">{supplier.status}</span></td><td><input type="checkbox" checked={supplier.active} onChange={() => toggleSupplierActive(supplier.id, supplier.active)} disabled={toggling[supplier.id]} /></td><td>{supplier.categories?.map((c) => c.name).join(", ")}</td><td>{supplier.rating || "—"}</td></tr>)}</tbody></table></div> : <p className="admin-empty">{t("noSuppliers")}</p>}
  </section>;
}

function ServicePricingField({ value, onChange }) {
  const { t } = useTranslation();
  return <label>{t("pricingType")}<span className="service-pricing-select">
    <select value={value || ""} onChange={(event) => onChange(event.target.value)}>
      <option value="">{t("selectPricingType")}</option>
      {Object.entries(pricingTypeLabels).map(([type, label]) => <option key={type} value={type}>{t(label)}</option>)}
    </select>
    <ChevronDown size={16} aria-hidden="true" />
  </span></label>;
}

function AdminDashboard({ onNotice }) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState("overview");
  const translationLanguages = [
    { code: "en", label: "English" },
    { code: "fr", label: "French" },
    { code: "el", label: "Greek" },
    { code: "ar", label: "Arabic" },
  ];
  const [metrics, setMetrics] = useState(null);
  const [categoriesList, setCategoriesList] = useState([]);
  const [pendingSuppliers, setPendingSuppliers] = useState([]);
  const [categoryTranslations, setCategoryTranslations] = useState(() => Object.fromEntries(
    translationLanguages.map(({ code }) => [code, { name: "", description: "" }]),
  ));
  const [editingCategory, setEditingCategory] = useState(null);
  const [loading, setLoading] = useState(true);

  async function refresh() {
    setLoading(true);
    try {
      const [analyticsResult, categoryResult, supplierResult] = await Promise.all([
        adminApi.analytics(), adminApi.categories(), adminApi.pendingSuppliers(),
      ]);
      setMetrics(analyticsResult.data);
      setCategoriesList(categoryResult.data);
      setPendingSuppliers(supplierResult.data);
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorLoadAdminData"));
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { refresh(); }, [t, onNotice]);

  async function addCategory(event) {
    event.preventDefault();
    const slug = categoryTranslations.en.name.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
    if (!slug) return;
    if (translationLanguages.some(({ code }) => !categoryTranslations[code].name.trim() || !categoryTranslations[code].description.trim())) {
      onNotice(t("categoryTranslationRequired"));
      return;
    }
    try {
      const translations = Object.fromEntries(translationLanguages.map(({ code }) => [code, {
        name: categoryTranslations[code].name.trim(),
        description: categoryTranslations[code].description.trim(),
      }]));
      const { data } = await adminApi.createCategory({
        name: translations.en.name,
        description: translations.en.description,
        slug,
        translations,
      });
      setCategoriesList((current) => [...current, data]);
      window.dispatchEvent(new Event("wedora:categories-updated"));
      setCategoryTranslations(Object.fromEntries(translationLanguages.map(({ code }) => [code, { name: "", description: "" }])));
      onNotice(t("categoryAdded"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorAddCategory"));
    }
  }

  async function saveCategoryTranslations(translations) {
    try {
      const { data } = await adminApi.updateCategory(editingCategory.id, {
        name: translations.en.name.trim(),
        description: translations.en.description.trim(),
        slug: editingCategory.slug,
        translations,
      });
      setCategoriesList((current) => current.map((category) => category.id === data.id ? data : category));
      setEditingCategory(null);
      window.dispatchEvent(new Event("wedora:categories-updated"));
      onNotice(t("categoryTranslationsSaved"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorSaveCategoryTranslations"));
    }
  }

  async function reviewSupplier(id, approved) {
    try {
      await adminApi.approveSupplier(id, approved);
      setPendingSuppliers((current) => current.filter((supplier) => supplier.id !== id));
      onNotice(approved ? t("supplierApproved") : t("supplierDeclined"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorUpdateSupplierApproval"));
    }
  }

  if (loading && !metrics) return <div className="payment-loading">{t("loadingPlatformOverview")}</div>;
  return <div className="admin-dashboard">
    <div className="admin-tabs"><button className={activeTab === "overview" ? "active" : ""} onClick={() => setActiveTab("overview")}>Overview</button><button className={activeTab === "suppliers" ? "active" : ""} onClick={() => setActiveTab("suppliers")}>{t("suppliers")}</button></div>
    {activeTab === "overview" && <div>
    <div className="dashboard-metrics">{Object.entries(metrics || {}).map(([key, value]) => <article key={key}><span>{t(`metric_${key}`, { defaultValue: key.replace(/([A-Z])/g, " $1") })}</span><strong>{value}</strong></article>)}</div>
    <section className="admin-panel"><div className="panel-heading"><div><h2>{t("supplierApplications")}</h2><p>{t("reviewBusinesses")}</p></div><ShieldCheck size={19} /></div>
      {pendingSuppliers.length ? pendingSuppliers.map((supplier) => <div className="admin-row" key={supplier.id}><div><strong>{supplier.businessName}</strong><small>{supplier.categoryName} · {supplier.location}</small></div><button className="booking-action" onClick={() => reviewSupplier(supplier.id, false)}>{t("decline")}</button><button className="primary-button" onClick={() => reviewSupplier(supplier.id, true)}>{t("approve")} <Check size={13} /></button></div>) : <p className="admin-empty">{t("noPendingApplications")}</p>}
    </section>
    <section className="admin-panel"><div className="panel-heading"><div><h2>{t("supplierCategories")}</h2><p>{t("manageDirectoryCategories")}</p></div><SlidersHorizontal size={19} /></div>
      <form className="category-add-form category-translation-form" onSubmit={addCategory}>
        {translationLanguages.map(({ code }) => <fieldset key={code}><legend>{t(`language_${code}`)}</legend>
          <input value={categoryTranslations[code].name} onChange={(event) => setCategoryTranslations((current) => ({ ...current, [code]: { ...current[code], name: event.target.value } }))} placeholder={t("categoryNameLanguage", { language: code })} maxLength={90} required />
          <textarea value={categoryTranslations[code].description} onChange={(event) => setCategoryTranslations((current) => ({ ...current, [code]: { ...current[code], description: event.target.value } }))} placeholder={t("categoryDescriptionLanguage", { language: code })} maxLength={300} required rows="2" />
        </fieldset>)}
        <button className="primary-button">{t("addCategory")} <ArrowRight size={14} /></button>
      </form>
      {categoriesList.map((category) => <div className="admin-row category-admin-row" key={category.id}><div><strong>{category.name}</strong><small>/{category.slug}{category.description ? ` · ${category.description}` : ""}</small></div><span className="status-pill">{category.active ? t("active") : t("archived")}</span><button className="booking-action" onClick={() => setEditingCategory(category)}>{t("editTranslations")}</button>{category.active && <button className="booking-action" onClick={async () => { try { await adminApi.archiveCategory(category.id); setCategoriesList((items) => items.map((item) => item.id === category.id ? { ...item, active: false } : item)); window.dispatchEvent(new Event("wedora:categories-updated")); onNotice(t("categoryArchived")); } catch (error) { onNotice(error.response?.data?.detail || t("errorArchiveCategory")); } }}>{t("archive")}</button>}</div>)}
    </section>
    </div>}
    {activeTab === "suppliers" && <SuppliersAdminPanel onNotice={onNotice} />}
    {editingCategory && <CategoryTranslationsModal category={editingCategory} onClose={() => setEditingCategory(null)} onSave={saveCategoryTranslations} />}
  </div>;
}

function CategoryTranslationsModal({ category, onClose, onSave }) {
  const { t } = useTranslation();
  const languages = [
    { code: "en", label: "English" },
    { code: "fr", label: "French" },
    { code: "el", label: "Greek" },
    { code: "ar", label: "Arabic" },
  ];
  const [translations, setTranslations] = useState(() => Object.fromEntries(languages.map(({ code }) => [
    code,
    {
      name: category.translations?.[code]?.name || (code === "en" ? category.name : ""),
      description: category.translations?.[code]?.description || (code === "en" ? category.description || "" : ""),
    },
  ])));
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal category-translations-modal" role="dialog" aria-modal="true" aria-label={t("editCategoryTranslations")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <div className="eyebrow">{t("multilingualCategory")}</div><h2>{t("editCategory", { slug: category.slug })}</h2>
      <form onSubmit={(event) => { event.preventDefault(); onSave(translations); }}>
        {languages.map(({ code }) => <fieldset key={code}><legend>{t(`language_${code}`)}</legend>
          <label>{t("name")}<input value={translations[code].name} maxLength={90} required onChange={(event) => setTranslations((current) => ({ ...current, [code]: { ...current[code], name: event.target.value } }))} /></label>
          <label>{t("description")}<textarea value={translations[code].description} maxLength={300} required rows="2" onChange={(event) => setTranslations((current) => ({ ...current, [code]: { ...current[code], description: event.target.value } }))} /></label>
        </fieldset>)}
        <button className="primary-button submit-booking">{t("saveTranslations")} <Check size={14} /></button>
      </form>
    </section>
  </div>;
}

function ChecklistCard({ tasks, categories, progress, currencyCode = "USD", onToggle, onEdit, onRemove, onOpen, expanded = false }) {
  const { t, i18n } = useTranslation();
  const totalBudgetCents = tasks.reduce((total, task) => total + Math.round(Number(task.budgetAmount || 0) * 100), 0);
  const formatBudget = (amount) => new Intl.NumberFormat(i18n.language, { style: "currency", currency: currencyCode }).format(amount);
  return <section className={`checklist-card${expanded ? " expanded" : ""}`}>
    <div className="checklist-head"><div className="check-icon"><ListChecks size={18} /></div><div><h3>{t("planningProgress")}</h3><p>{t("littleThingsAddUp")}</p></div><button className="text-button" onClick={onOpen}>{expanded ? t("yourList").toUpperCase() : t("viewList")} <ArrowRight size={14} /></button></div>
    <div className="progress-summary"><div className="progress-track"><span style={{ width: `${progress}%` }} /></div><span>{progress}%</span></div>
    <div className="progress-caption">{t("tasksDone", { completed: tasks.filter((task) => task.done).length, total: tasks.length })}</div>
    <div className="checklist-budget-total"><span>{t("totalBudget")}</span><strong>{formatBudget(totalBudgetCents / 100)}</strong></div>
    <div className="task-list">{tasks.slice(0, expanded ? undefined : 3).map((task) => <div className={`task-row${task.done ? " task-done" : ""}`} key={task.id}>
      <button className="task-check" onClick={() => onToggle(task.id)} disabled={task.collaborationSources?.length > 0} aria-label={task.done ? t("markIncomplete") : t("markComplete")}>{task.done && <Check size={13} />}</button>
      <div className="task-copy"><span>{task.title}</span><small className="task-category">{categories.find((category) => category.slug === task.category)?.name || (task.category === "custom" ? t("general") : task.category)}</small>{task.budgetAmount != null && <small className="task-budget">{t("taskBudget")}: {formatBudget(task.budgetAmount)}</small>}{task.collaborationSources?.map((source) => <small className="task-collaboration" key={`${source.collaborationId}-${source.serviceId}`}><Users size={12} /> {t("collaborationServiceSource", { serviceName: source.serviceName, collaborationName: source.collaborationName })}</small>)}{task.supplier && <small><CheckCircle2 size={12} /> {t("bookedWith", { supplier: task.supplier })}</small>}{task.contract && expanded && <small><ShieldCheck size={12} /> {task.contract} · {task.payment}</small>}</div>
      <span className="task-due">{task.due}</span>
      {expanded && !task.derivedFromCollaboration && <div className="task-actions">
        <button className="task-edit" onClick={() => onEdit(task)} aria-label={t("editTaskNamed", { title: task.title })} title={t("editTask")}><Pencil size={14} /></button>
        <button className="task-remove" onClick={() => onRemove(task.id)} aria-label={t("removeTaskNamed", { title: task.title })} title={t("removeTask")}><Trash2 size={14} /></button>
      </div>}
    </div>)}</div>
    {!expanded && <button className="checklist-footer" onClick={onOpen}>{t("littleAtATime")} <ArrowRight size={14} /></button>}
  </section>;
}

function ChecklistModal({ task, categories, currencyCode = "USD", onClose, onSubmit }) {
  const { t } = useTranslation();
  const [title, setTitle] = useState(task?.title || "");
  const [categorySlug, setCategorySlug] = useState(task?.category === "Custom" ? "custom" : task?.category || categories[0]?.slug || "custom");
  const [budgetAmount, setBudgetAmount] = useState(task?.budgetAmount == null ? "" : String(task.budgetAmount));
  const [saving, setSaving] = useState(false);

  async function submit(event) {
    event.preventDefault();
    const taskTitle = title.trim();
    if (!taskTitle || saving) return;
    setSaving(true);
    const saved = await onSubmit(taskTitle, categorySlug, budgetAmount === "" ? null : Number(budgetAmount));
    setSaving(false);
    if (saved) onClose();
  }

  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal checklist-modal" role="dialog" aria-modal="true" aria-label={task ? t("editChecklistTask") : t("addChecklistTask")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <div className="booking-modal-icon"><ListChecks size={20} /></div>
      <div className="eyebrow">{t("makeItYourOwn")}</div>
      <h2>{task ? t("editChecklistTask") : t("addChecklistTask")}</h2>
      <p>{task ? t("editChecklistTaskIntro") : t("addChecklistTaskIntro")}</p>
      <form onSubmit={submit}>
        <label>{t("taskName")}<input autoFocus maxLength={180} value={title} onChange={(event) => setTitle(event.target.value)} placeholder={t("taskNameExample")} required /></label>
        <label>{t("supplierCategory")}<select value={categorySlug} onChange={(event) => setCategorySlug(event.target.value)}>
          {categories.map((category) => <option key={category.slug} value={category.slug}>{category.name}</option>)}
          <option value="custom">{t("general")}</option>
        </select></label>
        <label>{t("taskBudget")} ({currencyCode})<input type="number" min="0" step="0.01" value={budgetAmount} onChange={(event) => setBudgetAmount(event.target.value)} placeholder="0.00" /></label>
        <button className="primary-button submit-booking" type="submit" disabled={saving}>{saving ? t("addingTask") : task ? t("saveTaskChanges") : t("addTask")} <Check size={14} /></button>
      </form>
    </section>
  </div>;
}

function CurrencyPreferenceCard({ currencyCode, onSave }) {
  const { t } = useTranslation();
  const [selectedCurrency, setSelectedCurrency] = useState(currencyCode || "USD");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setSelectedCurrency(currencyCode || "USD");
  }, [currencyCode]);

  async function submit(event) {
    event.preventDefault();
    if (saving || selectedCurrency === currencyCode) return;
    setSaving(true);
    await onSave(selectedCurrency);
    setSaving(false);
  }

  return <section className="currency-preference-card">
    <div><strong>{t("currencyPreference")}</strong><p>{t("currencyPreferenceIntro")}</p><small>{t("currencyConversionNotice")}</small></div>
    <form onSubmit={submit}>
      <select aria-label={t("currencyPreference")} value={selectedCurrency} onChange={(event) => setSelectedCurrency(event.target.value)}>
        <option value="USD">{t("currencyUSD")}</option>
        <option value="EUR">{t("currencyEUR")}</option>
      </select>
      <button className="primary-button" type="submit" disabled={saving || selectedCurrency === currencyCode}>{saving ? t("saving") : t("saveCurrencyPreference")}</button>
    </form>
  </section>;
}

function EmptyState({ title, detail, action, actionLabel }) {
  const { t } = useTranslation();
  return <div className="empty-state"><span><Flower2 size={25} /></span><h3>{title}</h3><p>{detail}</p>{action && <button className="secondary-button" onClick={action}>{actionLabel || t("browseSuppliers")} <ArrowRight size={14} /></button>}</div>;
}

function BookingRow({ booking, role, payment, reviewed, onNotice, onDetails, onPay, onReview, onUpdated, onConfirmed }) {
  const { t } = useTranslation();
  async function acceptQuote() {
    try {
      const { data } = await bookingsApi.confirm(booking.id);
      onUpdated(data);
      onConfirmed(data);
      onNotice(t("bookingConfirmedChecklistUpdated"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorConfirmBooking"));
    }
  }

  async function completeBooking() {
    try {
      const { data } = await bookingsApi.complete(booking.id);
      onUpdated(data);
      onNotice(t("bookingCompleteReviewAvailable"));
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorCompleteBooking"));
    }
  }

  async function downloadInvoice() {
    try {
      const { data } = await paymentsApi.invoice(booking.id);
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = `${data.invoiceNumber}.json`;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
    } catch (error) {
      onNotice(error.response?.data?.detail || t("errorCreateInvoice"));
    }
  }

  const paymentLabel = payment?.status === "SUCCEEDED" ? t("paid")
    : payment?.status === "PENDING" ? t("paymentProcessing")
      : payment?.status === "REFUNDED" ? t("refunded")
        : payment?.status === "REFUND_PENDING" ? t("refundProcessing") : t(`status_${booking.status}`, { defaultValue: booking.status });
  const canRetryPayment = !payment || payment.status === "FAILED" || payment.status === "REFUNDED";
  return <div className="booking-row">
    <div className="booking-avatar"><CheckCircle2 size={19} /></div>
    <div className="booking-main"><strong>{role === "SUPPLIER" ? booking.customerName || t("couple") : booking.supplierName}</strong><span>{booking.eventDate}{booking.serviceOfferingName ? ` · ${booking.serviceOfferingName}` : ""} · {t(`status_${booking.status}`, { defaultValue: booking.status })}{booking.quotedAmountCents ? ` · ${(booking.quotedAmountCents / 100).toFixed(2)} ${booking.currency}` : ""}</span></div>
    <span className="status-pill">{paymentLabel}</span>
    {booking.status === "QUOTED" && role === "CUSTOMER" && <button className="booking-action" onClick={acceptQuote}>{t("acceptQuote")}</button>}
    {role === "SUPPLIER" && <button className="booking-action booking-details-action" onClick={onDetails}>{booking.status === "REQUESTED" ? t("reviewRequest") : t("viewDetails")}</button>}
    {role === "CUSTOMER" && <button className="booking-action booking-details-action" onClick={onDetails}>{t("viewDetails")}</button>}
    {["CONFIRMED", "ACTIVE", "CLOSED"].includes(booking.status) && role === "CUSTOMER" && payment?.status === "SUCCEEDED" && <button className="booking-action" onClick={downloadInvoice}>{t("downloadInvoice")}</button>}
    {["CONFIRMED", "WAITING_PAYMENT"].includes(booking.status) && role === "CUSTOMER" && canRetryPayment && <button className="booking-action" onClick={onPay}>{t("paySecurely")}</button>}
    {booking.status === "CONFIRMED" && role === "SUPPLIER" && <button className="booking-action" onClick={completeBooking}>{t("markComplete")}</button>}
    {["COMPLETED", "CLOSED"].includes(booking.status) && role === "CUSTOMER" && <button className="booking-action" disabled={reviewed} onClick={onReview}>{reviewed ? t("reviewed") : t("leaveReview")}</button>}
    <ChevronRight size={17} />
  </div>;
}

function BookingDetailsModal({ booking, role, onClose, onQuote, onAccept, onDecline, onWorkflow }) {
  const { t } = useTranslation();
  const [comment, setComment] = useState("");
  const [saving, setSaving] = useState(false);
  async function workflow(action, needsComment = false) {
    if (needsComment && !comment.trim()) return;
    setSaving(true);
    try {
      if (await onWorkflow(action, needsComment ? comment.trim() : null)) setComment("");
    } finally {
      setSaving(false);
    }
  }
  const supplierCanRespond = role === "SUPPLIER"
    && ["REQUESTED", "ACCEPTED", "QUOTED"].includes(booking.status);
  const canAcceptRequest = role === "SUPPLIER" && booking.status === "REQUESTED";
  const canMakeOffer = role === "SUPPLIER" && ["REQUESTED", "ACCEPTED"].includes(booking.status);
  const canDecline = supplierCanRespond;
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal booking-details-modal" role="dialog" aria-modal="true" aria-label={t("bookingRequestDetails")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <div className="booking-modal-icon"><CalendarDays size={20} /></div>
      <div className="eyebrow">{role === "SUPPLIER" ? t("coupleRequest").toUpperCase() : t("bookingDetails").toUpperCase()}</div>
      <h2>{role === "SUPPLIER" ? t("requestFrom", { name: booking.customerName || t("aCouple") }) : booking.supplierName}</h2>
      <div className="booking-detail-grid">
        <div><span>{t("eventDate")}</span><strong>{booking.eventDate}</strong></div>
        <div><span>{t("status")}</span><strong>{t(`status_${booking.status}`, { defaultValue: booking.status })}</strong></div>
        {booking.serviceOfferingName && <div><span>{t("selectedService")}</span><strong>{booking.serviceOfferingName}</strong></div>}
        {booking.quantity != null && <div><span>{t("requestQuantity")}</span><strong>{booking.quantity}</strong></div>}
        {booking.serviceOfferingName && <div><span>{t("servicePrice")}</span><strong>{booking.servicePrice == null ? "—" : `${booking.servicePrice} ${booking.servicePriceCurrency || ""}`} {pricingTypeLabels[booking.servicePricingType] ? `· ${t(pricingTypeLabels[booking.servicePricingType])}` : ""}</strong></div>}
        {booking.quotedAmountCents && <div><span>{t(["WAITING_PAYMENT", "ACTIVE", "CLOSED"].includes(booking.status) ? "price" : "offer")}</span><strong>{(booking.quotedAmountCents / 100).toFixed(2)} {booking.currency}</strong></div>}
      </div>
      <div className="booking-request-notes"><strong>{t("notesFromCouple")}</strong><p>{booking.notes?.trim() || t("noAdditionalRequestDetails")}</p></div>
      {supplierCanRespond && <div className="booking-detail-actions">
        {canAcceptRequest && <button className="secondary-button" onClick={onAccept}><Check size={15} /> {t("acceptRequest")}</button>}
        {canDecline && <button className="booking-action" onClick={onDecline}>{t("decline")}</button>}
        {canMakeOffer && <button className="primary-button" onClick={onQuote}>{t("makeOffer")} <ArrowRight size={14} /></button>}
      </div>}
      {role === "CUSTOMER" && booking.status === "ACCEPTED" && <p className="booking-detail-hint">{t("supplierAcceptedHint")}</p>}
      {role === "CUSTOMER" && booking.status === "QUOTED" && <p className="booking-detail-hint">{t("supplierQuoteHint")}</p>}
      {(role === "SUPPLIER" && ["NEW_REQUEST", "UNDER_REVIEW", "ACCEPTED_BY_COUPLE"].includes(booking.status) || role === "CUSTOMER" && booking.status === "SENT_TO_COUPLE") && <div className="booking-workflow">
        <label>{t("requestComment")}<textarea value={comment} maxLength={3000} onChange={(event) => setComment(event.target.value)} /></label>
        <div className="booking-detail-actions">
          {role === "SUPPLIER" && booking.status === "NEW_REQUEST" && <button className="primary-button" disabled={saving} onClick={() => workflow("review")}>{t("moveUnderReview")}</button>}
          {role === "SUPPLIER" && booking.status === "UNDER_REVIEW" && <button className="primary-button" disabled={saving} onClick={onQuote}>{t("makeOffer")}</button>}
          {role === "SUPPLIER" && booking.status === "ACCEPTED_BY_COUPLE" && <button className="primary-button" disabled={saving} onClick={() => workflow("finalize")}>{t("approveForPayment")}</button>}
          {role === "CUSTOMER" && booking.status === "SENT_TO_COUPLE" && <><button className="primary-button" disabled={saving} onClick={() => workflow("acceptQuotation")}>{t("acceptQuote")}</button><button className="secondary-button" disabled={saving || !comment.trim()} onClick={() => workflow("review", true)}>{t("sendBackForReview")}</button></>}
          <button className="secondary-button" disabled={saving || !comment.trim()} onClick={() => workflow("declineRequest", true)}>{t("decline")}</button>
        </div>
      </div>}
      {booking.history?.length > 0 && <section><h3>{t("requestHistory")}</h3><ol>{booking.history.map((entry, index) => <li key={index}><strong>{t(`status_${entry.status}`, { defaultValue: entry.status })}</strong> · {t(`actor_${entry.actor}`, { defaultValue: entry.actor })} · <time>{new Date(entry.createdAt).toLocaleString()}</time>{entry.comment && <p>{entry.comment}</p>}</li>)}</ol></section>}
    </section>
  </div>;
}

function SupplierModal({ supplier, countryOptions, saved, canContact, onSave, onClose, onBook, onMessage }) {
  const { t, i18n } = useTranslation();
  const [media, setMedia] = useState([]);
  const [serviceOffers, setServiceOffers] = useState([]);
  const [servicesLoading, setServicesLoading] = useState(true);
  const [servicesError, setServicesError] = useState(false);
  const [selectedMedia, setSelectedMedia] = useState(null);
  const [mediaLoading, setMediaLoading] = useState(true);
  const [mediaError, setMediaError] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewsError, setReviewsError] = useState(false);

  useEffect(() => {
    let active = true;
    setMedia([]);
    setMediaLoading(true);
    setMediaError(false);
    suppliersApi.media(supplier.id).then(({ data }) => {
      if (active) setMedia(data);
    }).catch(() => {
      if (active) setMediaError(true);
    }).finally(() => {
      if (active) setMediaLoading(false);
    });
    setServiceOffers([]);
    setServicesLoading(true);
    setServicesError(false);
    supplierServicesApi.list(supplier.id).then(({ data }) => {
      if (active) setServiceOffers(data);
    }).catch(() => {
      if (active) setServicesError(true);
    }).finally(() => {
      if (active) setServicesLoading(false);
    });
    setReviews([]);
    setReviewsLoading(true);
    setReviewsError(false);
    reviewsApi.list(supplier.id).then(({ data }) => {
      if (active) setReviews(data);
    }).catch(() => {
      if (active) setReviewsError(true);
    }).finally(() => {
      if (active) setReviewsLoading(false);
    });
    return () => { active = false; };
  }, [i18n.language, supplier.id]);

  const cover = media.find((item) => item.mediaType.startsWith("image/"));
  const profileLink = (value) => {
    if (!value) return null;
    if (/^[a-z][a-z\d+.-]*:/i.test(value) && !/^https?:\/\//i.test(value)) return null;
    return /^https?:\/\//i.test(value) ? value : `https://${value}`;
  };
  const socialProfileLink = (platform, value) => {
    if (!value) return null;
    if (value.startsWith("@")) {
      const base = platform === "facebook" ? "https://www.facebook.com" : `https://www.${platform}.com`;
      return `${base}/${encodeURIComponent(value.slice(1))}`;
    }
    return profileLink(value);
  };
  const websiteUrl = profileLink(supplier.website);
  const whatsappNumber = supplier.whatsapp?.replace(/\D/g, "");
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="supplier-modal" role="dialog" aria-modal="true" aria-label={t("supplierDetails", { name: supplier.name })}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <img className="modal-cover" src={imageUrl(supplier.mainImageUrl || cover?.url || supplier.image, 1000)} alt={t("supplierPhotoAlt", { category: supplierCategoryLabel(t, supplier.category), name: supplier.name })} />
      <div className="modal-body">
        <div className="supplier-category">{supplierCategoryLabel(t, supplier.category)} <span>·</span> {supplier.place}</div>
        <h2>{supplier.name} {supplier.verified !== false && <span className="verified"><Check size={12} /></span>}</h2>
        <div className="modal-rating"><Star size={15} fill="currentColor" /> {supplier.rating || t("newSupplier")} <span>{t("reviewCount", { count: supplier.reviews || 0 })}</span>{supplier.approved !== false && <span className="verified-label"><ShieldCheck size={14} /> {t("verifiedSupplier")}</span>}</div>
        {supplier.description && <p className="modal-description">{supplier.description}</p>}
        <div className="supplier-profile-details">
          {supplier.location && <div><MapPin size={15} /><span>{supplier.location}</span></div>}
          {supplier.countryCodes?.length > 0 && <div className="supplier-profile-wide"><Globe size={15} /><span>{supplier.countryCodes.map((code) => localizedCountryName(code, i18n.language, countryOptions)).join(", ")}</span></div>}
          {supplier.latitude != null && supplier.longitude != null && <div className="supplier-profile-wide supplier-map-details"><strong>{t("supplierMapLocation")}</strong><SupplierLocationMap value={{ latitude: supplier.latitude, longitude: supplier.longitude }} label={t("supplierMapLocation")} /></div>}
          {supplier.startingPrice != null && <div><strong>{t("startingPrice")}</strong><span>{supplier.currency} {supplier.startingPrice}</span></div>}
          {supplier.services && <div className="supplier-profile-wide"><strong>{t("services")}</strong><p>{supplier.services}</p></div>}
          {supplier.availability && <div className="supplier-profile-wide"><strong>{t("availability")}</strong><p>{supplier.availability}</p></div>}
          {supplier.contactPhone && <a href={`tel:${supplier.contactPhone}`}><Phone size={15} /><span>{supplier.contactPhone}</span></a>}
          {supplier.contactEmail && <a href={`mailto:${encodeURIComponent(supplier.contactEmail)}`}><Mail size={15} /><span>{supplier.contactEmail}</span></a>}
          {whatsappNumber && <a href={`https://wa.me/${whatsappNumber}`} target="_blank" rel="noreferrer"><MessageCircle size={15} /><span>{t("whatsapp")}</span></a>}
          {websiteUrl && <a href={websiteUrl} target="_blank" rel="noreferrer"><Globe size={15} /><span>{supplier.website}</span></a>}
          {supplier.facebook && socialProfileLink("facebook", supplier.facebook) && <a href={socialProfileLink("facebook", supplier.facebook)} target="_blank" rel="noreferrer"><Globe size={15} /><span>{t("facebook")}</span></a>}
          {supplier.instagram && socialProfileLink("instagram", supplier.instagram) && <a href={socialProfileLink("instagram", supplier.instagram)} target="_blank" rel="noreferrer"><Globe size={15} /><span>{t("instagram")}</span></a>}
          {supplier.tiktok && socialProfileLink("tiktok", supplier.tiktok) && <a href={socialProfileLink("tiktok", supplier.tiktok)} target="_blank" rel="noreferrer"><Globe size={15} /><span>{t("tiktok")}</span></a>}
        </div>
          <section className="supplier-services">
            <h3>{t("supplierServices")}</h3>
            {servicesLoading ? <p className="supplier-section-message">{t("loadingServices")}</p>
              : servicesError ? <p className="supplier-section-message" role="alert">{t("servicesFailed")}</p>
                : serviceOffers.length ? <div className="supplier-service-grid">{serviceOffers.map((service) => (
                  <article className="supplier-service-card" key={service.id}>
                    {service.imageUrl && <img src={resolveMediaUrl(service.imageUrl)} alt={service.name} loading="lazy" />}
                    <div className="supplier-service-card-body">
                      <h4>{service.name}</h4>
                      {service.categoryName && <small>{service.categoryName}</small>}
                      {service.description && <p>{service.description}</p>}
                      <strong>{service.price != null ? `${service.currency} ${service.price}` : t("priceOnRequest")}</strong>
                      {pricingTypeLabels[service.pricingType] && <p>{t(pricingTypeLabels[service.pricingType])}</p>}
                      <p>{t("serviceQuantityRange", { min: service.minimumAmount, max: service.maximumAmount })}</p>
                      {canContact && <button className="secondary-button" onClick={() => onBook(service)}>{t("requestThisService")}</button>}
                    </div>
                  </article>
                ))}</div>
                  : <p className="supplier-section-message">{t("noServicesYet")}</p>}
          </section>
          <section className="supplier-gallery">
          <div className="supplier-gallery-heading"><h3>{t("portfolioMedia")}</h3>{!mediaLoading && !mediaError && <span>{t("mediaCount", { count: media.length })}</span>}</div>
          {mediaLoading ? <p className="supplier-section-message">{t("loadingSupplierMedia")}</p>
            : mediaError ? <p className="supplier-section-message" role="alert">{t("supplierMediaFailed")}</p>
              : media.length ? <div className="supplier-media-grid">{media.map((item) => {
                const url = resolveMediaUrl(item.url);
                return item.mediaType.startsWith("video/")
                  ? <figure key={item.id}><video src={url} controls preload="metadata" aria-label={item.caption || t("supplierPortfolioVideo")} /><figcaption>{item.caption || t("supplierVideo")}</figcaption></figure>
                  : <figure key={item.id}><button className="supplier-media-open" onClick={() => setSelectedMedia(item)} aria-label={t("viewSupplierPhoto", { caption: item.caption || t("supplierPortfolioPhoto") })}><img src={url} alt={item.caption || t("supplierPortfolio")} loading="lazy" /></button><figcaption>{item.caption || t("supplierPhoto")}</figcaption></figure>;
              })}</div>
                : <p className="supplier-section-message">{t("noSupplierMedia")}</p>}
        </section>
        <section className="supplier-reviews">
          <h3>{t("reviews")}</h3>
          {reviewsLoading ? <p className="supplier-section-message">{t("loadingReviews")}</p>
            : reviewsError ? <p className="supplier-section-message" role="alert">{t("reviewsFailed")}</p>
              : reviews.length ? reviews.map((review) => <article className="supplier-review" key={review.id}>
                <div><strong>{review.authorName}</strong><span><Star size={12} fill="currentColor" /> {review.rating}/5</span></div>
                {review.comment && <p>{review.comment}</p>}
              </article>)
                : <p className="supplier-section-message">{t("noReviewsYet")}</p>}
        </section>
        <div className="modal-price"><span>{supplier.price === t("askForQuote") ? t("pricing") : t("startingFrom")}</span><strong>{formatSupplierPrice(t, { ...supplier, pricePrefix: false })}</strong><small>{t("askAboutPackages")}</small></div>
        {canContact && <div className="modal-actions"><button className="primary-button" onClick={() => onBook(null, serviceOffers)}>{t(serviceOffers.length ? "requestBookingWithOption" : "requestBooking")} <ArrowRight size={15} /></button><button className="secondary-button" onClick={onMessage}><Mail size={15} /> {t("sendMessage")}</button><button className={`modal-heart${saved ? " is-saved" : ""}`} onClick={() => onSave(supplier.id)} aria-label={saved ? t("removeSavedSupplier") : t("saveSupplier")}><Heart size={18} fill={saved ? "currentColor" : "none"} /></button></div>}
      </div>
    </section>
    {selectedMedia && <div className="media-viewer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setSelectedMedia(null)}>
      <section className="media-viewer" role="dialog" aria-modal="true" aria-label={selectedMedia.caption || "Supplier portfolio photo"}>
        <button className="modal-close" onClick={() => setSelectedMedia(null)} aria-label={t("closePhoto")}><X size={19} /></button>
        <img src={resolveMediaUrl(selectedMedia.url)} alt={selectedMedia.caption || `${supplier.name} portfolio`} />
        {selectedMedia.caption && <p>{selectedMedia.caption}</p>}
      </section>
    </div>}
  </div>;
}

function BookingModal({ supplier, serviceOffering, options = [], onClose, onSubmit }) {
  const { t, i18n } = useTranslation();
  const [date, setDate] = useState("");
  const [quantity, setQuantity] = useState("");
  const [notes, setNotes] = useState("");
  const [selectedOptionId, setSelectedOptionId] = useState(serviceOffering?.id || options[0]?.id || "");
  const [calendarMonth, setCalendarMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const [availabilityDates, setAvailabilityDates] = useState([]);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [availabilityError, setAvailabilityError] = useState(false);
  const selectedOffering = options.find((option) => String(option.id) === String(selectedOptionId))
    || (serviceOffering && String(serviceOffering.id) === String(selectedOptionId) ? serviceOffering : null);

  useEffect(() => {
    if (!selectedOffering) {
      setAvailabilityDates([]);
      setAvailabilityError(false);
      return undefined;
    }
    let active = true;
    const from = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), 1);
    const to = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() + 1, 0);
    setAvailabilityLoading(true);
    setAvailabilityError(false);
    supplierServicesApi.availability(
      supplier.id, selectedOffering.id, localDateString(from), localDateString(to),
    ).then(({ data }) => {
      if (active) setAvailabilityDates(data);
    }).catch(() => {
      if (active) setAvailabilityError(true);
    }).finally(() => {
      if (active) setAvailabilityLoading(false);
    });
    return () => { active = false; };
  }, [calendarMonth, selectedOffering?.id, supplier.id]);

  const monthFirstDay = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), 1);
  const monthDays = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() + 1, 0).getDate();
  const calendarOffset = monthFirstDay.getDay();
  const calendarCells = Math.ceil((calendarOffset + monthDays) / 7) * 7;
  const unavailableDates = new Set(availabilityDates.map((item) => item.date));
  const today = localDateString(new Date());

  function submit(event) {
    event.preventDefault();
    if (!date) return;
    onSubmit({ eventDate: date, quantity: quantity ? Number(quantity) : null, notes, serviceOfferingId: selectedOffering?.id });
  }
  const locale = i18n.language === "el" ? "el-GR" : i18n.language === "fr" ? "fr-FR" : i18n.language === "ar" ? "ar" : "en-US";
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="booking-modal" role="dialog" aria-modal="true" aria-label={t("requestBooking")}><button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button><div className="booking-modal-icon"><CalendarDays size={21} /></div><div className="eyebrow">{t("bookingEyebrow")}</div><h2>{t("askAboutDate")}</h2><p>{t("bookingIntro", { name: supplier.name })}</p>
    <form onSubmit={submit}>
      {options.length > 0 && <label>{t("chooseService")}<select value={selectedOptionId}
        onChange={(event) => {
          setSelectedOptionId(event.target.value);
          setQuantity("");
          setDate("");
          setAvailabilityDates([]);
        }} required>
        <option value="" disabled>{t("chooseService")}</option>
        {options.map((option) => <option value={option.id} key={option.id}>{option.name}</option>)}
      </select></label>}
      {selectedOffering && <p className="selected-option-label">{t("selectedService")}: <strong>{selectedOffering.name}</strong></p>}
      {selectedOffering && <p>{t("servicePrice")}: {selectedOffering.price == null ? "—" : `${selectedOffering.price} ${supplier.currency || ""}`} {pricingTypeLabels[selectedOffering.pricingType] ? `· ${t(pricingTypeLabels[selectedOffering.pricingType])}` : ""}</p>}
      {selectedOffering ? <section className="availability-calendar" aria-label={t("serviceAvailabilityCalendar")}>
        <div className="availability-calendar-heading">
          <button type="button" className="booking-action" aria-label={t("previousMonth")} disabled={calendarMonth.getFullYear() === new Date().getFullYear() && calendarMonth.getMonth() <= new Date().getMonth()} onClick={() => { setCalendarMonth((month) => new Date(month.getFullYear(), month.getMonth() - 1, 1)); setDate(""); }}>‹</button>
          <strong>{calendarMonth.toLocaleDateString(locale, { month: "long", year: "numeric" })}</strong>
          <button type="button" className="booking-action" aria-label={t("nextMonth")} onClick={() => { setCalendarMonth((month) => new Date(month.getFullYear(), month.getMonth() + 1, 1)); setDate(""); }}>›</button>
        </div>
        <div className="availability-calendar-grid">
          {Array.from({ length: 7 }, (_, index) => <span className="availability-weekday" key={`weekday-${index}`}>{new Date(2024, 0, 7 + index).toLocaleDateString(locale, { weekday: "short" })}</span>)}
          {Array.from({ length: calendarCells }, (_, index) => {
            const day = index - calendarOffset + 1;
            if (day < 1 || day > monthDays) return <span className="availability-empty" key={`empty-${index}`} />;
            const dayDate = localDateString(new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), day));
            const unavailable = unavailableDates.has(dayDate);
            return <button type="button" key={dayDate} className={`availability-day${date === dayDate ? " selected" : ""}${unavailable ? " unavailable" : ""}`}
              disabled={availabilityLoading || unavailable || dayDate < today}
              aria-label={`${dayDate}${unavailable ? `, ${t("unavailable")}` : ""}`}
              aria-pressed={date === dayDate}
              onClick={() => setDate(dayDate)}>{day}</button>;
          })}
        </div>
        {availabilityLoading && <p className="supplier-section-message">{t("loadingAvailability")}</p>}
        {availabilityError && <p className="auth-error" role="alert">{t("availabilityFallback")}</p>}
        <small>{t("unavailable")}: {t("unavailableDatesHint")}</small>
        {!date && <span className="calendar-date-required">{t("chooseAvailableDate")}</span>}
      </section> : <label>{t("weddingDate")}<input type="date" lang={i18n.language} value={date} onChange={(event) => setDate(event.target.value)} required min={today} /></label>}
      <label>{t("requestQuantity")}<input type="number" min={selectedOffering?.minimumAmount || 1} max={selectedOffering?.maximumAmount} step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
      <label>{t("noteToSupplier", { name: supplier.name })} <span className="optional">({t("optional")})</span><textarea rows="3" maxLength={2000} placeholder={t("bookingNotePlaceholder")} value={notes} onChange={(event) => setNotes(event.target.value)} /></label>
      <div className="booking-note"><ShieldCheck size={15} /> {t("enquiryNoPayment")}</div><button className="primary-button submit-booking" type="submit" disabled={!date || (selectedOffering && availabilityLoading)}>{t("sendBookingRequest")} <ArrowRight size={15} /></button>
    </form></section></div>;
}

function AuthModal({
  account,
  initialMode,
  countryOptions,
  countryLoadStatus,
  onRetryCountries,
  onSavePhoneNumber,
  onClose,
  onSignedIn,
  onSignOut,
}) {
  const { t } = useTranslation();
  const [mode, setMode] = useState(initialMode);
  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
  const [loginIdentifier, setLoginIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [location, setLocation] = useState("");
  const [countryCodes, setCountryCodes] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [savingPhone, setSavingPhone] = useState(false);
  const [phoneError, setPhoneError] = useState("");
  const [usernameStatus, setUsernameStatus] = useState("idle");

  useEffect(() => {
    setPhoneNumber(account?.phoneNumber || "");
  }, [account?.phoneNumber]);

  useEffect(() => {
    if (mode === "login") {
      setUsernameStatus("idle");
      return undefined;
    }
    const normalizedUsername = username.trim();
    if (!normalizedUsername) {
      setUsernameStatus("idle");
      return undefined;
    }
    if (!/^[a-zA-Z0-9._-]{3,30}$/.test(normalizedUsername)) {
      setUsernameStatus("invalid");
      return undefined;
    }
    let active = true;
    setUsernameStatus("checking");
    const timeout = window.setTimeout(() => {
      authApi.usernameAvailable(normalizedUsername).then(({ data }) => {
        if (active) setUsernameStatus(data ? "available" : "taken");
      }).catch(() => {
        if (active) setUsernameStatus("error");
      });
    }, 350);
    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [mode, username]);

  async function submit(event) {
    event.preventDefault();
    if (mode !== "login" && usernameStatus === "taken") {
      setError(t("usernameTaken"));
      return;
    }
    if (mode !== "login" && usernameStatus === "checking") return;
    if (mode !== "login" && usernameStatus === "invalid") {
      setError(t("usernameFormat"));
      return;
    }
    if (mode !== "login" && countryCodes.length === 0) {
      setError(t("countriesRequired"));
      return;
    }
    setLoading(true);
    setError("");
    try {
      const { data } = mode === "login"
        ? await authApi.login({ username: loginIdentifier, password })
        : mode === "supplier"
          ? await authApi.registerSupplier({ name, username, password, businessName, location, countryCodes, phoneNumber: phoneNumber.trim() || null })
          : await authApi.register({ name, username, password, countryCodes, phoneNumber: phoneNumber.trim() || null });
      onSignedIn(data);
    } catch (requestError) {
      const response = requestError.response;
      const status = response?.status;
      const detail = response?.data?.detail || response?.data?.message;
      if (detail) setError(detail);
      else if (status === 401) setError(t("loginCredentialsRejected"));
      else if (status === 403) setError(t("loginForbidden"));
      else if (status) setError(t("loginServerError", { status }));
      else setError(t("loginServerUnreachable"));
    } finally {
      setLoading(false);
    }
  }

  async function savePhoneNumber(event) {
    event.preventDefault();
    setSavingPhone(true);
    setPhoneError("");
    try {
      if (await onSavePhoneNumber(phoneNumber.trim())) setPhoneError("");
      else setPhoneError(t("errorSavePhoneNumber"));
    } finally {
      setSavingPhone(false);
    }
  }

  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal auth-modal" role="dialog" aria-modal="true" aria-label={t("yourWedoraAccount")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      {account ? <>
        <div className="eyebrow">{t("yourWedoraAccount")}</div><h2>{t("lovelyToSeeYou", { name: account.name })}</h2>
        <p>{t("signedInPrivate", { username: account.username || account.email })}</p>
        <form onSubmit={savePhoneNumber}>
          <label>{t("phoneNumber")} <span className="optional">({t("optional")})</span>
            <input type="tel" autoComplete="tel" placeholder="+15551234567" value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)} maxLength={16}
              pattern="\+[1-9][0-9]{7,14}" />
          </label>
          <p>{t("whatsappPhoneHint")}</p>
          {phoneError && <p className="auth-error" role="alert">{phoneError}</p>}
          <button className="primary-button submit-booking" type="submit" disabled={savingPhone}>
            {savingPhone ? t("saving") : t("savePhoneNumber")}
          </button>
        </form>
        <button className="secondary-button" onClick={onSignOut}>{t("signOut")}</button>
      </> : <>
        <div className="booking-modal-icon"><Heart size={20} /></div><div className="eyebrow">{t("littlePlaceForPlans")}</div>
        <h2>{mode === "login" ? t("welcomeBackShort") : mode === "supplier" ? t("shareLovelyWork") : t("getStarted")}</h2>
        <p>{mode === "login" ? t("signInKeepPlans") : mode === "supplier" ? t("supplierProfileReviewInfo") : t("createPrivateAccountInfo")}</p>
        <form onSubmit={submit}>
          {mode !== "login" && <label>{t("yourName")}<input autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} required maxLength={120} /></label>}
          {mode === "login"
            ? <label>{t("usernameOrEmail")}<input type="text" autoComplete="username" value={loginIdentifier} onChange={(event) => setLoginIdentifier(event.target.value)} required maxLength={180} /></label>
            : <>
              <label>{t("username")}<input type="text" autoComplete="username" value={username}
                onChange={(event) => { setUsername(event.target.value); setError(""); }}
                minLength={3} maxLength={30} pattern="[a-zA-Z0-9._-]+" required /></label>
              {usernameStatus !== "idle" && <p className={usernameStatus === "available" ? "username-availability available" : "username-availability"} role="status">{t(`usernameStatus_${usernameStatus}`)}</p>}
            </>}
          <label>{t("password")}<input type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} value={password} onChange={(event) => setPassword(event.target.value)} minLength={10} required maxLength={72} /></label>
          {mode !== "login" && <label>{t("phoneNumber")} <span className="optional">({t("optional")})</span>
            <input type="tel" autoComplete="tel" placeholder="+15551234567" value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)} maxLength={16}
              pattern="\+[1-9][0-9]{7,14}" />
          </label>}
          {mode !== "login" && <p>{t("whatsappPhoneHint")}</p>}
          {mode === "supplier" && <>
            <label>{t("businessName")}<input value={businessName} onChange={(event) => setBusinessName(event.target.value)} required maxLength={150} /></label>
            <label>{t("businessLocation")}<input value={location} onChange={(event) => setLocation(event.target.value)} required maxLength={180} /></label>
          </>}
          {mode !== "login" && <CountryMultiSelect
            label={t(mode === "supplier" ? "countriesServed" : "yourCountries")}
            countryOptions={countryOptions}
            countryLoadStatus={countryLoadStatus}
            onRetryCountries={onRetryCountries}
            value={countryCodes}
            onChange={setCountryCodes}
            initiallyOpen
          />}
          {error && <p className="auth-error" role="alert">{error}</p>}
          <button className="primary-button submit-booking" type="submit" disabled={loading}>{loading ? t("oneMoment") : mode === "login" ? t("landingSignIn") : mode === "supplier" ? t("applyAsSupplier") : t("createYourAccount")} <ArrowRight size={14} /></button>
        </form>
        {mode === "login"
          ? <div className="auth-switch-row"><button className="auth-switch" onClick={() => { setMode("register"); setError(""); }}>{t("createCoupleAccount")}</button><button className="auth-switch" onClick={() => { setMode("supplier"); setError(""); }}>{t("joinAsSupplier")}</button></div>
          : <button className="auth-switch" onClick={() => { setMode("login"); setError(""); }}>{t("alreadyHaveAccount")}</button>}
      </>}
    </section>
  </div>;
}

function PaymentModal({ bookingId, onClose, onPaid }) {
  const { t } = useTranslation();
  const [payment, setPayment] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    paymentsApi.createIntent(bookingId)
      .then(({ data }) => setPayment(data))
      .catch((requestError) => setError(requestError.response?.data?.detail || t("errorStartPayment")));
  }, [bookingId, t]);

  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal payment-modal" role="dialog" aria-modal="true" aria-label={t("securePayment")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <div className="booking-modal-icon"><ShieldCheck size={20} /></div><div className="eyebrow">{t("secureCheckout")}</div>
      <h2>{t("paymentLovelyDetail")}</h2><p>{t("stripeSecureInfo")}</p>
      {!import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY && <p className="auth-error">{t("checkoutNotConfigured")}</p>}
      {error && <p className="auth-error" role="alert">{error}</p>}
      {payment?.clientSecret && stripePromise && <Elements stripe={stripePromise} options={{ clientSecret: payment.clientSecret }}>
        <StripePaymentForm onPaid={onPaid} />
      </Elements>}
      {!payment && !error && <div className="payment-loading">{t("preparingCheckout")}</div>}
    </section>
  </div>;
}

function StripePaymentForm({ onPaid }) {
  const { t } = useTranslation();
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    if (!stripe || !elements) return;
    setSubmitting(true);
    setError("");
    const result = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: window.location.href },
      redirect: "if_required",
    });
    if (result.error) {
      setError(result.error.message || t("paymentNotCompleted"));
      setSubmitting(false);
    } else {
      onPaid();
    }
  }

  return <form className="stripe-payment-form" onSubmit={submit}>
    <PaymentElement />
    {error && <p className="auth-error" role="alert">{error}</p>}
    <button className="primary-button submit-booking" type="submit" disabled={!stripe || submitting}>{submitting ? t("processingSecurely") : t("paySecurely")}</button>
  </form>;
}

function QuoteModal({ booking, onClose, onSubmit }) {
  const { t } = useTranslation();
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState(booking.currency || "USD");
  function submit(event) {
    event.preventDefault();
    const amountCents = Math.round(Number(amount) * 100);
    if (!Number.isSafeInteger(amountCents) || amountCents <= 0) return;
    onSubmit({ amountCents, currency });
  }
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal" role="dialog" aria-modal="true" aria-label={t("sendBookingQuote")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <div className="booking-modal-icon"><Mail size={20} /></div><div className="eyebrow">{t("thoughtfulNextStep")}</div>
      <h2>{t("sendYourQuote")}</h2><p>{t("quoteIntro")}</p>
      <form onSubmit={submit}>
        <label>{t("quoteAmount")}<input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required /></label>
        <label>{t("currency")}<select value={currency} onChange={(event) => setCurrency(event.target.value)}><option>EUR</option><option>USD</option><option>GBP</option></select></label>
        <button className="primary-button submit-booking" type="submit">{t("sendQuote")} <ArrowRight size={14} /></button>
      </form>
    </section>
  </div>;
}

function ReviewModal({ booking, onClose, onSubmit }) {
  const { t } = useTranslation();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="booking-modal" role="dialog" aria-modal="true" aria-label={t("leaveSupplierReview")}>
      <button className="modal-close" onClick={onClose} aria-label={t("closeDialog")}><X size={19} /></button>
      <div className="booking-modal-icon"><Star size={20} /></div><div className="eyebrow">{t("reviewEyebrow")}</div>
      <h2>{t("experienceQuestion")}</h2><p>{t("reviewIntro", { name: booking.supplierName })}</p>
      <form onSubmit={(event) => { event.preventDefault(); onSubmit({ rating, comment }); }}>
        <label>{t("yourRating")}<select value={rating} onChange={(event) => setRating(Number(event.target.value))}><option value={5}>{t("rating5")}</option><option value={4}>{t("rating4")}</option><option value={3}>{t("rating3")}</option><option value={2}>{t("rating2")}</option><option value={1}>{t("rating1")}</option></select></label>
        <label>{t("aFewWords")} <span className="optional">({t("optional")})</span><textarea rows="4" maxLength={2000} value={comment} onChange={(event) => setComment(event.target.value)} placeholder={t("reviewPlaceholder")} /></label>
        <button className="primary-button submit-booking">{t("shareReview")} <ArrowRight size={14} /></button>
      </form>
    </section>
  </div>;
}

function ChatInbox({ bookings: bookingList, user, initialBookingId, onNotice }) {
  const { t, i18n } = useTranslation();
  const [selectedId, setSelectedId] = useState(initialBookingId || bookingList[0]?.id || null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");
  const [state, setState] = useState("connecting");
  const [client, setClient] = useState(null);
  const [connectionAttempt, setConnectionAttempt] = useState(0);
  const [uploadingFile, setUploadingFile] = useState(false);
  const selectedBooking = bookingList.find((booking) => booking.id === selectedId);

  useEffect(() => {
    if (initialBookingId && bookingList.some((booking) => booking.id === initialBookingId)) {
      setSelectedId(initialBookingId);
    }
  }, [bookingList, initialBookingId]);

  useEffect(() => {
    if (!selectedId) return undefined;
    let active = true;
    const token = localStorage.getItem("wedora-token");
    const stompClient = new Client({
      brokerURL: websocketUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        if (!active) return;
        setState("connected");
        stompClient.subscribe(`/topic/bookings/${selectedId}`, (frame) => {
          const message = JSON.parse(frame.body);
          setMessages((current) => current.some((item) => item.id === message.id) ? current : [...current, message]);
        });
      },
      onStompError: (frame) => {
        if (active) {
          setState("connectionAttention");
          onNotice(frame.headers.message || t("chatConnectionFailed"));
        }
      },
      onWebSocketError: (event) => {
        console.error("Chat WebSocket connection failed", event);
        if (active) {
          setState("connectionAttention");
          onNotice(t("chatConnectionFailed"));
        }
      },
      onWebSocketClose: () => active && setState("reconnecting"),
    });
    setClient(stompClient);
    setMessages([]);
    setState("loadingMessages");
    messagesApi.history(selectedId).then(({ data }) => {
      if (active) setMessages(data);
    }).catch((error) => {
      if (active) setState(error.response?.data?.detail || t("conversationLoadFailed"));
    });
    stompClient.activate();
    return () => {
      active = false;
      stompClient.deactivate();
      setClient(null);
    };
  }, [connectionAttempt, selectedId, t]);

  function reconnect() {
    setState("connecting");
    setConnectionAttempt((attempt) => attempt + 1);
  }

  function send(event) {
    event.preventDefault();
    const body = draft.trim();
    if (!body || !client?.connected || !selectedId) return;
    client.publish({ destination: `/app/bookings/${selectedId}`, body: JSON.stringify({ body }) });
    setDraft("");
  }

  async function uploadFile(event) {
    const input = event.currentTarget;
    const file = input.files?.[0];
    if (!file || !selectedId || !client?.connected) return;
    const formData = new FormData();
    formData.append("file", file);
    setUploadingFile(true);
    try {
      const { data } = await messagesApi.uploadAttachment(selectedId, formData);
      client.publish({
        destination: `/app/bookings/${selectedId}`,
        body: JSON.stringify({ body: "", attachmentId: data.id }),
      });
    } catch (error) {
      setState(error.response?.data?.detail || t("errorShareFile"));
    } finally {
      setUploadingFile(false);
      input.value = "";
    }
  }

  async function downloadFile(url) {
    try {
      const { data } = await messagesApi.downloadAttachment(url);
      const objectUrl = URL.createObjectURL(data);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = "wedding-attachment";
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
    } catch (error) {
      setState(error.response?.data?.detail || t("errorDownloadAttachment"));
    }
  }

  return <section className="chat-panel">
    <aside className="chat-conversations"><div className="chat-list-title"><div><h2>{t("yourConversations")}</h2><p>{t("weddingTeamConversations")}</p></div><MessageCircle size={19} /></div>
      {bookingList.map((booking) => <button key={booking.id} className={`chat-conversation${selectedId === booking.id ? " selected" : ""}`} onClick={() => setSelectedId(booking.id)}><span className="chat-person-avatar">{booking.supplierName?.slice(0, 1) || "♡"}</span><span><strong>{booking.supplierName}</strong><small>{booking.eventDate} · {t(`status_${booking.status}`, { defaultValue: booking.status.toLowerCase() })}</small></span></button>)}
    </aside>
    <div className="chat-thread">
      {selectedBooking ? <>
        <div className="chat-thread-head"><div className="chat-person-avatar">{selectedBooking.supplierName?.slice(0, 1) || "♡"}</div><div><strong>{selectedBooking.supplierName}</strong><small><i className={state === "connected" ? "online-dot" : ""} /> {t(`chat_${state}`)}</small></div>{state !== "connected" && <button className="chat-reconnect-button" onClick={reconnect} aria-label={t("retryChatConnection")}><RotateCcw size={15} /></button>}<ShieldCheck size={17} /></div>
        <div className="chat-messages">{messages.map((message) => <div key={message.id} className={`chat-bubble${message.senderId === user.id ? " own" : ""}`}>{message.body && <span>{message.body}</span>}{message.attachmentUrl && <button className="attachment-link" onClick={() => downloadFile(message.attachmentUrl)}><Paperclip size={13} /> {t("downloadSharedFile")}</button>}<small>{new Date(message.createdAt).toLocaleString(i18n.language)}</small></div>)}{messages.length === 0 && <div className="chat-start-note"><Sparkles size={16} /><span>{t("conversationStartPrompt")}</span></div>}</div>
        <form className="chat-compose" onSubmit={send}><label className="chat-attach-button" title={t("shareSupportedFile")}><Paperclip size={17} /><input type="file" accept="image/jpeg,image/png,image/webp,video/mp4,application/pdf" onChange={uploadFile} disabled={!client?.connected || uploadingFile} aria-label={t("shareSupportedFile")} /></label><input value={draft} onChange={(event) => setDraft(event.target.value)} placeholder={t("writeMessage")} maxLength={4000} /><button className="primary-button" disabled={!client?.connected || !draft.trim()} type="submit">{t("send")} <ArrowRight size={14} /></button></form>
      </> : <div className="chat-start-note">{t("chooseConversation")}</div>}
    </div>
  </section>;
}

export default App;
