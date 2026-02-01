import { makeAutoObservable } from "mobx";
import { type User } from "../model/User";

class AuthStore {
    private readonly TOKEN_KEY = 'access_token';

    token: string | null = localStorage.getItem(this.TOKEN_KEY);
    isAuthenticated: boolean = false;
    user: User | null = null;
    
    constructor() {
        makeAutoObservable(this);
    }

    setAuthToken(token: string | null) {
        this.token = token;
        this.isAuthenticated = !!token;
        if (token) {
            localStorage.setItem(this.TOKEN_KEY, token);
        } else {
            localStorage.removeItem(this.TOKEN_KEY);
        }
    }

    setUser(user: User | null) {
        this.user = user;
    }

    loadUser() {
        if (!this.token) return;

    }

    logout() {
        this.setAuthToken(null)
        this.setUser(null);
    }
}

export const authStore = new AuthStore();