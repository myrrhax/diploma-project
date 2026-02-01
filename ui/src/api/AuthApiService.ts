import type AuthResponse from "../model/AuthResponse";
import $api from "./AxiosClient";
import { authStore } from "../store/AuthStore";
import type ErrorResponse from "../model/ErrorResponse";
import type AuthRequest from "../model/AuthRequest";

export default class AuthApiService {
    async login(request: AuthRequest): Promise<ErrorResponse | null> {
        return this.authenticate(request, '/auth/login');
    }

    async register(request: AuthRequest): Promise<ErrorResponse | null> {
        return this.authenticate(request, '/auth/register');
    }

    private async authenticate(request: AuthRequest, url: string): Promise<ErrorResponse | null> {
        try {
            const response = await $api.post<AuthResponse | ErrorResponse>(url, request);
            if (response.status === 200 && response.data) {
                const data = response.data as AuthResponse;
                authStore.setAuthToken(data.accessToken);
                authStore.setUser(data.user);

                return null;
            } else {
                const error = response.data as ErrorResponse;
                console.error("Failed to login. Reason: " + error.message);
                
                return error;
            }
        } catch(e) {
            console.error("Failed to send auth request. Reason: " + e);
            
            throw e;
        }
    }
}