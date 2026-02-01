import type AuthResponse from "../model/AuthResponse";
import type LoginRequest from "../model/LoginRequest";
import $api from "./AxiosClient";
import { authStore } from "../store/AuthStore";
import type ErrorResponse from "../model/ErrorResponse";

export default class AuthApiService {
    async login(request: LoginRequest): Promise<ErrorResponse | null> {
        try {
            const response = await $api.post<AuthResponse | ErrorResponse>('/auth/login', request);
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
            
            return null;
        }
    }
}