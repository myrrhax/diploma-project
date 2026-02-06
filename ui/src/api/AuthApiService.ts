import type AuthResponse from "../model/AuthResponse";
import $api from "./AxiosClient";
import { authStore } from "../store/AuthStore";
import type ErrorResponse from "../model/ErrorResponse";
import type AuthRequest from "../model/AuthRequest";
import axios from "axios";
import type { User } from "../model/User";

class AuthApiService {
    async login(request: AuthRequest): Promise<ErrorResponse | null> {
        return this.authenticate(request, '/auth/login');
    }

    async register(request: AuthRequest): Promise<ErrorResponse | null> {
        return this.authenticate(request, '/auth/register');
    }

    async fetchUser(): Promise<User | null> {
        try {
            const response = await $api.get<User>('/users/whoami');
            if (response.status === 200 && response.data) {
                return response.data;
            }

            return null;
        } catch(e: any) {
            this.processApiError(e);

            return null;
        }
    }

    async confirmEmail(confirmationCode: string): Promise<ErrorResponse | null> {
        try {
            const response = await $api.post('/auth/confirm', {confirmationCode: confirmationCode});
            console.log(response);
            const data = response.data as AuthResponse;
            this.updateUserInfo(data);

            return null;
        } catch (e: any) {
            return this.processApiError(e);
        }
    }

    async resendCode(): Promise<boolean> {
        try {
            const response = await $api.post('/auth/resend-code');
            
            return response.status === 200;
        } catch (e: any) {
            console.error('Failed to resend code');
            console.error(e);
            
            return false;
        }
    }

    private async authenticate(request: AuthRequest, url: string): Promise<ErrorResponse | null> {
        try {
            const response = await $api.post<AuthResponse | ErrorResponse>(url, request);
            console.log(response);
            const data = response.data as AuthResponse;
            this.updateUserInfo(data);

            return null;
        } catch(e: any) {
            return this.processApiError(e);
        }
    }

    private processApiError(e: Error): ErrorResponse {
        if (axios.isAxiosError(e)) {
            const serverError = e.response?.data as ErrorResponse;
            return serverError || { message: 'Ошибка на стороне сервера' }
        }

        console.error('Сетевая ошибка или сервер временно не доступен');
        throw e;
    }

    private updateUserInfo(data: AuthResponse) {
        authStore.setUser(data.user);
        authStore.setAuthToken(data.accessToken);
    }
}

export const authApi = new AuthApiService(); 