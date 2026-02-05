import type AuthResponse from "../model/AuthResponse";
import $api from "./AxiosClient";
import { authStore } from "../store/AuthStore";
import type ErrorResponse from "../model/ErrorResponse";
import type AuthRequest from "../model/AuthRequest";
import axios from "axios";

class AuthApiService {
    async login(request: AuthRequest): Promise<ErrorResponse | null> {
        return this.authenticate(request, '/auth/login');
    }

    async register(request: AuthRequest): Promise<ErrorResponse | null> {
        return this.authenticate(request, '/auth/register');
    }

    private async authenticate(request: AuthRequest, url: string): Promise<ErrorResponse | null> {
        try {
            const response = await $api.post<AuthResponse | ErrorResponse>(url, request);
            console.log(response);
            const data = response.data as AuthResponse;
            authStore.setAuthToken(data.accessToken);
            authStore.setUser(data.user);

            return null;
        } catch(e: any) {
            if (axios.isAxiosError(e)) {
                const serverError = e.response?.data as ErrorResponse;
                return serverError || { message: "Неизвестная ошибка сервера" };
            }
            
            console.error("Сетевая ошибка или сервер недоступен:", e.message);
            throw e;
        }
    }
}

export const authApi = new AuthApiService(); 