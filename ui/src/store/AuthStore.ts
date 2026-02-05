import { makeAutoObservable } from "mobx";
import { type User } from "../model/User";

class AuthStore {
    private readonly TOKEN_KEY = 'access_token';

    token: string | null = localStorage.getItem(this.TOKEN_KEY);
    user: User | null = null;
    
    constructor() {
        makeAutoObservable(this);
    }

    public setAuthToken(token: string | null) {
        this.token = token;
        if (token) {
            localStorage.setItem(this.TOKEN_KEY, token);
        } else {
            localStorage.removeItem(this.TOKEN_KEY);
        }
    }

    public setUser(user: User | null) {
        this.user = user;
    }

    public loadUser() {
        if (!this.token) return;

    }

    public logout() {
        this.setAuthToken(null)
        this.setUser(null);
    }

    public get isAuthenticated(): boolean {
        return this.user !== null;
    }
}

export const authStore = new AuthStore();