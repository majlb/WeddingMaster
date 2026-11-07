import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  timeout: 10000,
});
export function resolveMediaUrl(url) {
  if (!url) return "";
  const apiBase = new URL(import.meta.env.VITE_API_URL || "/api", window.location.origin);
  return new URL(url, apiBase).toString();
}
export const websocketUrl = import.meta.env.VITE_WS_URL
  || `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}/ws`;

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("wedora-token");
  const isAuthenticationRequest = config.url?.startsWith("/auth/");
  if (token && !isAuthenticationRequest) config.headers.Authorization = `Bearer ${token}`;
  else if (isAuthenticationRequest) delete config.headers.Authorization;
  config.headers["Accept-Language"] = localStorage.getItem("wedora-language") || "en";
  return config;
});

export const suppliersApi = {
  list: (params) => api.get("/suppliers", { params }),
  get: (id) => api.get(`/suppliers/${id}`),
  getBySlug: (slug) => api.get(`/suppliers/slug/${encodeURIComponent(slug)}`),
  owned: () => api.get("/suppliers/me", { headers: { "Accept-Language": "en" } }),
  update: (id, profile) => api.put(`/suppliers/${id}`, profile, { headers: { "Accept-Language": "en" } }),
  analytics: (id) => api.get(`/suppliers/me/${id}/analytics`),
  media: (id) => api.get(`/suppliers/${id}/media`),
  uploadMedia: (id, formData) => api.post(`/suppliers/${id}/media`, formData),
  uploadMainImage: (id, formData) => api.post(`/suppliers/${id}/media/main-image`, formData),
  updateCategories: (id, categoryIds) => api.put(`/suppliers/${id}/categories`, { categoryIds }),
};
export const supplierServicesApi = {
  list: (supplierId) => api.get(`/suppliers/${supplierId}/services`),
  listOwned: (supplierId, language) => api.get(`/suppliers/me/${supplierId}/services`, {
    headers: language ? { "Accept-Language": language } : undefined,
  }),
  create: (supplierId, formData) => api.post(`/suppliers/${supplierId}/services`, formData),
  update: (supplierId, serviceId, formData) => api.put(`/suppliers/${supplierId}/services/${serviceId}`, formData),
  remove: (supplierId, serviceId) => api.delete(`/suppliers/${supplierId}/services/${serviceId}`),
  availability: (supplierId, serviceId, from, to) => api.get(
    `/suppliers/${supplierId}/services/${serviceId}/availability`, { params: { from, to } },
  ),
  ownedAvailability: (supplierId, serviceId, from, to) => api.get(
    `/suppliers/me/${supplierId}/services/${serviceId}/availability`, { params: { from, to } },
  ),
  blockDate: (supplierId, serviceId, date) => api.post(
    `/suppliers/me/${supplierId}/services/${serviceId}/availability`, { date },
  ),
  unblockDate: (supplierId, serviceId, date) => api.delete(
    `/suppliers/me/${supplierId}/services/${serviceId}/availability/${date}`,
  ),
};
export const collaborationsApi = {
  list: () => api.get("/collaborations"),
  listPublic: () => api.get("/collaborations/public"),
  create: (collaboration) => api.post("/collaborations", collaboration),
  activate: (collaborationId) => api.post(`/collaborations/${collaborationId}/activate`),
  invite: (collaborationId, supplierId) => api.post(
    `/collaborations/${collaborationId}/invitations`, { supplierId },
  ),
  removeInvite: (collaborationId, supplierId) => api.delete(
    `/collaborations/${collaborationId}/members/${supplierId}`,
  ),
  respond: (collaborationId, accepted) => api.post(
    `/collaborations/${collaborationId}/response`, { accepted },
  ),
  addItem: (collaborationId, serviceId) => api.post(
    `/collaborations/${collaborationId}/items`, { serviceId },
  ),
  addItems: (collaborationId, serviceIds) => api.post(
    `/collaborations/${collaborationId}/items/batch`, { serviceIds },
  ),
  select: (collaborationId) => api.post(`/collaborations/${collaborationId}/select`),
  unselect: (collaborationId) => api.delete(`/collaborations/${collaborationId}/select`),
};
export const authApi = {
  register: (details) => api.post("/auth/register", details),
  registerSupplier: (details) => api.post("/auth/register-supplier", details),
  login: (credentials) => api.post("/auth/login", credentials),
  usernameAvailable: (username) => api.get("/auth/username-availability", { params: { username } }),
};
export const categoriesApi = {
  list: () => api.get("/categories"),
};
export const countriesApi = {
  list: () => api.get("/countries"),
};
export const usersApi = {
  updateCountries: (countryCodes) => api.put("/users/me/countries", { countryCodes }),
  updateWeddingDate: (weddingDate) => api.put("/users/me/wedding-date", { weddingDate }),
  updateCurrency: (currencyCode) => api.put("/users/me/currency", { currencyCode }),
};
export const assistantApi = {
  chat: (messages) => api.post("/assistant/chat", { messages }, { timeout: 195000 }),
};
export const bookingsApi = {
  list: () => api.get("/bookings"),
  request: (booking) => api.post("/bookings", booking),
  quote: (id, quote) => api.post(`/bookings/${id}/quote`, quote),
  accept: (id) => api.post(`/bookings/${id}/accept`),
  decline: (id) => api.post(`/bookings/${id}/decline`),
  confirm: (id) => api.post(`/bookings/${id}/confirm`),
  complete: (id) => api.post(`/bookings/${id}/complete`),
};
export const checklistApi = {
  list: () => api.get("/checklist"),
  add: (task) => api.post("/checklist", task),
  update: (id, task) => api.put(`/checklist/${id}`, task),
  setCompleted: (id, completed) => api.patch(`/checklist/${id}`, null, { params: { completed } }),
  remove: (id) => api.delete(`/checklist/${id}`),
};
export const paymentsApi = {
  history: () => api.get("/payments"),
  createIntent: (bookingId) => api.post(`/bookings/${bookingId}/payments`),
  invoice: (bookingId) => api.get(`/bookings/${bookingId}/invoice`),
};
export const reviewsApi = {
  list: (supplierId) => api.get(`/suppliers/${supplierId}/reviews`),
  create: (bookingId, review) => api.post(`/bookings/${bookingId}/reviews`, review),
};
export const messagesApi = {
  history: (bookingId) => api.get(`/bookings/${bookingId}/messages`),
  uploadAttachment: (bookingId, formData) => api.post(`/bookings/${bookingId}/attachments`, formData),
  downloadAttachment: (url) => api.get(url, { responseType: "blob" }),
};
export const notificationsApi = {
  list: () => api.get("/notifications"),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put("/notifications/read-all"),
};
export const adminApi = {
  analytics: () => api.get("/admin/analytics"),
  categories: () => api.get("/admin/categories", { headers: { "Accept-Language": "en" } }),
  createCategory: (category) => api.post("/admin/categories", category, { headers: { "Accept-Language": "en" } }),
  updateCategory: (id, category) => api.put(`/admin/categories/${id}`, category, { headers: { "Accept-Language": "en" } }),
  archiveCategory: (id) => api.delete(`/admin/categories/${id}`),
  pendingSuppliers: () => api.get("/admin/suppliers/pending"),
  approveSupplier: (id, approved) => api.patch(`/admin/suppliers/${id}/approval`, null, { params: { approved } }),
};
