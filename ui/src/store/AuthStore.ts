import { makeAutoObservable } from "mobx";
import { type User } from "../model/User";
import { authApi } from "../api/AuthApiService";

class AuthStore {
    private readonly TOKEN_KEY = 'access_token';

    isLoading: boolean = true;
    token: string | null = localStorage.getItem(this.TOKEN_KEY);
    user: User | null = null;
    
    constructor() {
        makeAutoObservable(this);
    }

    async init() {
        if (this.token) {
            console.debug('User has token, trying to fetch user info')
            this.isLoading = true;
            
            const fetchedUser = await authApi.fetchUser();
            console.debug('Fetch user info: ' + fetchedUser)
            if (fetchedUser) {
                this.setUser(fetchedUser);
            }
        }
        this.isLoading = false;
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

    public logout() {
        this.setAuthToken(null)
        this.setUser(null);
    }

    public get isAuthenticated(): boolean {
        return this.user !== null;
    }
}

export const authStore = new AuthStore();