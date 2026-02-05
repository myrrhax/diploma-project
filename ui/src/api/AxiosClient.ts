import axios from "axios";
import { authStore } from "../store/AuthStore";
import type AuthResponse from "../model/AuthResponse";

export const baseURL = 'http://localhost:8000/api'

const $api = axios.create({
    baseURL: baseURL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
});

$api.interceptors.request.use(
    (cfg) => {
        const token = authStore.token;
        if (token && cfg.headers) {
            cfg.headers.Authorization = `Bearer ${token}`;
        }

        return cfg; 
    },
    (error) => {
        return Promise.reject(error);
    }
);

$api.interceptors.response.use(
    (resp) => {
        return resp;
    },
    async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401) {
            if (!originalRequest._isRetry) {
                originalRequest._isRetry = true;

                try {
                    const response = await axios.post<AuthResponse>(baseURL + '/auth/refresh', { withCredentials: true });
                    if (response.status === 200 && response.data) {
                        authStore.setAuthToken(response.data.accessToken);
                        authStore.setUser(response.data.user);
                    }

                    return $api.request(originalRequest);
                } catch(e) {
                    console.error(e);
                    authStore.logout();

                    return Promise.reject(e);
                }
            } else {
                authStore.logout();

                return Promise.reject(error);
            }
        }

        return Promise.reject(error);
    }
);

export default $api;