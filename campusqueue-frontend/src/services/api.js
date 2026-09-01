/**
 * CampusQueue Centralized REST API Utility
 * Connects directly to the Spring Boot backend REST endpoints with session credentials.
 */

const API_BASE_URL = 'http://localhost:8081/api';

/**
 * Core fetch wrapper with centralized error handling, JSON parsing, and session cookies.
 */
async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const config = {
    // Crucial for Spring Security session cookie (JSESSIONID) transmission
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);

    // 204 No Content
    if (response.status === 204) {
      return null;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMessage = data?.message || data?.error || `HTTP error ${response.status}`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.details = data?.details || [];
      error.payload = data;
      throw error;
    }

    return data;
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      const networkError = new Error('Cannot connect to CampusQueue backend server (localhost:8081). Please ensure Spring Boot is running.');
      networkError.status = 0;
      throw networkError;
    }
    throw err;
  }
}

// ==========================================
// 1. Authentication APIs
// ==========================================

export async function login(email, password) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export async function getMe() {
  return request('/auth/me');
}

export async function logout() {
  return request('/auth/logout', {
    method: 'POST',
  });
}

// ==========================================
// 2. User APIs
// ==========================================

export async function getAllUsers() {
  return request('/users');
}

export async function getUserById(id) {
  return request(`/users/${id}`);
}

export async function createUser(userData) {
  return request('/users', {
    method: 'POST',
    body: JSON.stringify(userData),
  });
}

// ==========================================
// 3. Counter APIs
// ==========================================

export async function getAllCounters() {
  return request('/counters');
}

export async function getActiveCounters() {
  return request('/counters/active');
}

export async function getCounterById(id) {
  return request(`/counters/${id}`);
}

export async function createCounter(counterData) {
  return request('/counters', {
    method: 'POST',
    body: JSON.stringify(counterData),
  });
}

export async function toggleCounterStatus(id) {
  return request(`/counters/${id}/toggle-status`, {
    method: 'PATCH',
  });
}

// ==========================================
// 4. Ticket & Queue APIs
// ==========================================

export async function createTicket(counterId, userId) {
  return request('/tickets', {
    method: 'POST',
    body: JSON.stringify({ counterId, userId }),
  });
}

export async function getTicketById(id) {
  return request(`/tickets/${id}`);
}

export async function getUserTickets(userId) {
  return request(`/tickets/user/${userId}`);
}

export async function getQueueStatus(counterId) {
  return request(`/tickets/counter/${counterId}/status`);
}

export async function getWaitingTickets(counterId) {
  return request(`/tickets/counter/${counterId}/waiting`);
}

export async function getCurrentTicket(counterId) {
  return request(`/tickets/counter/${counterId}/current`);
}

export async function callNextTicket(counterId) {
  return request(`/tickets/counter/${counterId}/call-next`, {
    method: 'POST',
  });
}

export async function callSpecificTicket(ticketId) {
  return request(`/tickets/${ticketId}/call`, {
    method: 'POST',
  });
}

export async function completeTicket(ticketId, remarks = '') {
  const query = remarks ? `?remarks=${encodeURIComponent(remarks)}` : '';
  return request(`/tickets/${ticketId}/complete${query}`, {
    method: 'POST',
  });
}

export async function skipTicket(ticketId, remarks = '') {
  const query = remarks ? `?remarks=${encodeURIComponent(remarks)}` : '';
  return request(`/tickets/${ticketId}/skip${query}`, {
    method: 'POST',
  });
}

export async function cancelTicket(ticketId) {
  return request(`/tickets/${ticketId}/cancel`, {
    method: 'POST',
  });
}

// ==========================================
// 5. Analytics APIs
// ==========================================

export async function getAnalyticsOverview() {
  return request('/analytics/overview');
}

export async function getDailyVolume() {
  return request('/analytics/daily-volume');
}

export async function getBusiestCounter() {
  return request('/analytics/busiest-counter');
}

export async function getPeakHour() {
  return request('/analytics/peak-hour');
}

export async function getCounterPerformance(date = null) {
  const query = date ? `?date=${encodeURIComponent(date)}` : '';
  return request(`/analytics/performance${query}`);
}

export async function getCounterStats(counterId) {
  return request(`/analytics/counters/${counterId}/stats`);
}

export async function getWaitingQueueOrdered(counterId) {
  return request(`/analytics/counters/${counterId}/queue`);
}
