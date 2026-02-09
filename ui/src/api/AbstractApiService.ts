import axios from "axios";
import type ErrorResponse from "../model/ErrorResponse";

export abstract class AbstractApiService {
    protected processApiError(e: Error): ErrorResponse {
        if (axios.isAxiosError(e)) {
            const serverError = e.response?.data as ErrorResponse;
            return serverError || { message: 'Ошибка на стороне сервера' }
        }

        console.error('Сетевая ошибка или сервер временно не доступен');
        throw e;
    }
}