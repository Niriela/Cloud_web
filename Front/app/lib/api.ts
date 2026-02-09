type ApiError = {
  message: string;
  status?: number;
};

const API_BASE =
  import.meta.env.VITE_API_BASE?.replace(/\/$/, "") ?? "http://localhost:8080";

async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = localStorage.getItem("auth_token");
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers ?? {}),
    },
    ...options,
  });

  if (!response.ok) {
    let message = "Request failed";
    try {
      const data = (await response.json()) as { message?: string };
      if (data?.message) {
        message = data.message;
      }
    } catch {
      try {
        const text = await response.text();
        if (text) {
          message = text;
        }
      } catch {
        // Ignore parse errors and use default message.
      }
    }

    const error: ApiError = { message, status: response.status };
    throw error;
  }

  return (await response.json()) as T;
}

export type AuthResponse = {
  token: string;
  type: string;
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  expiresAt?: string | null;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type SignalementMapDto = {
  id: number;
  latitude: number | null;
  longitude: number | null;
  date: string | null;
  description: string | null;
  surface: number | null;
  budget: number | null;
  statutsId: number | null;
  statut: string | null;
  entrepriseId: number | null;
  entreprise: string | null;
  typeSignalementId: number | null;
  typeSignalement: string | null;
};

export type TypeSignalement = {
  id: number;
  libelle: string;
};

export type Statut = {
  id: number;
  libelle: string;
};

export type Entreprise = {
  id: number;
  name: string;
  address: string | null;
  phone: string | null;
  active: boolean | null;
};

export type UserAdmin = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  date: string | null;
  failedLoginAttempts: number | null;
  statutsUser: string | null;
  userType: string | null;
};

export type SignalementsStats = {
  totalPoints: number;
  totalSurface: number;
  totalBudget: number;
  advancementPercent: number;
};

export function getAuthSession() {
  return apiRequest<AuthResponse>("/api/auth/session", {
    method: "GET",
  });
}

export function login(payload: LoginPayload) {
  return apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loginOffline(payload: LoginPayload) {
  return apiRequest<AuthResponse>("/api/auth/offline/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function createManagerUser(payload: RegisterPayload) {
  return apiRequest<UserAdmin>("/api/manager/users", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getSignalements(filters?: { status?: string; type?: string }) {
  const params = new URLSearchParams();
  if (filters?.status) params.set("status", filters.status);
  if (filters?.type) params.set("type", filters.type);
  const query = params.toString();
  return apiRequest<SignalementMapDto[]>(
    `/api/signalements${query ? `?${query}` : ""}`,
  );
}

export function getTypeSignalements() {
  return apiRequest<TypeSignalement[]>("/api/type-signalements");
}

export function getStatuts() {
  return apiRequest<Statut[]>("/api/statuts");
}

export function getEntreprises() {
  return apiRequest<Entreprise[]>("/api/entreprises");
}

export function getSignalementsStats(filters?: {
  status?: string;
  type?: string;
}) {
  const params = new URLSearchParams();
  if (filters?.status) params.set("status", filters.status);
  if (filters?.type) params.set("type", filters.type);
  const query = params.toString();
  return apiRequest<SignalementsStats>(
    `/api/signalements/stats${query ? `?${query}` : ""}`,
  );
}

export function updateSignalement(
  id: number,
  payload: {
    surface?: number | null;
    budget?: number | null;
    statutsId?: number | null;
    entrepriseId?: number | null;
    typeSignalementId?: number | null;
  },
) {
  return apiRequest<SignalementMapDto>(`/api/signalements/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deleteSignalement(id: number) {
  return apiRequest<void>(`/api/signalements/${id}`, {
    method: "DELETE",
  });
}

export function getUsers() {
  return apiRequest<UserAdmin[]>("/api/users");
}

export function resetUserBlock(userId: number) {
  return apiRequest<void>(`/api/auth/reset-block?userId=${userId}`, {
    method: "POST",
  });
}

export function updateUser(
  id: number,
  payload: { email?: string; firstName?: string; lastName?: string },
) {
  return apiRequest<UserAdmin>(`/api/users/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function syncFirebase() {
  return apiRequest<void>("/api/sync/firebase/refresh", {
    method: "POST",
  });
}
