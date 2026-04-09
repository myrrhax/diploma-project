import React, { useState, useRef, useEffect, useCallback } from 'react';
import { z } from 'zod';
import './css/ConfirmationPage.css'
import { authApi } from '../api/AuthApiService';

const otpSchema = z.string().length(6).regex(/^\d+$/);
const CODE_LENGTH = 6;
const RESEND_DELAY = 60;
const RESEND_TIMER_STORAGE_KEY = 'otp_resend_deadline';

export const AccountConfirmationPage = () => {
    const [otp, setOtp] = useState<string[]>(new Array(CODE_LENGTH).fill(""));
    const [apiError, setApiError] = useState<string | null>();
    const [timeLeft, setTimeLeft] = useState<number>(0);
    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
    const timerRef = useRef<number | null>(null);

    const tick = useCallback((deadline: number) => {
        if (timerRef.current) clearInterval(timerRef.current);

        timerRef.current = setInterval(() => {
            const now = Date.now();
            const remaining = Math.round((deadline - now) / 1000);

            if (remaining <= 0) {
                clearInterval(timerRef.current!);
                setTimeLeft(0);
                localStorage.removeItem(RESEND_TIMER_STORAGE_KEY);
            } else {
                setTimeLeft(remaining);
            }
        }, 1000) as unknown as number;
    }, []);

    const startTimer = useCallback((seconds: number) => {
        const deadline = Date.now() + seconds * 1000;
        localStorage.setItem(RESEND_TIMER_STORAGE_KEY, deadline.toString());
        setTimeLeft(seconds);
        tick(deadline);
    }, [tick]);

    useEffect(() => {
        const savedDeadline = localStorage.getItem(RESEND_TIMER_STORAGE_KEY);

        if (savedDeadline) {
            const parsedDeadline = parseInt(savedDeadline, 10);
            const now = Date.now();
            const remaining = Math.round((parsedDeadline - now) / 1000);

            if (remaining > 0) {
                setTimeLeft(remaining);
                tick(parsedDeadline);
            } else {
                localStorage.removeItem(RESEND_TIMER_STORAGE_KEY);
            }
        } else {
            startTimer(RESEND_DELAY);
        }

        return () => {
            if (timerRef.current) clearInterval(timerRef.current);
        }
    }, [startTimer, tick]);

    const clearInputs = () => {
        setOtp(new Array(CODE_LENGTH).fill(""));
        inputRefs.current[0]?.focus();
    }

    const submitCode = async (code: string) => {
        const result = otpSchema.safeParse(code);
        if (result.success) {
            try {
                const error = await authApi.confirmEmail(code);
                if (error) {
                    console.error('Failed to confirm email');
                    setApiError(error.message);
                    clearInputs();
                }
            } catch (e: any) {
                setApiError('Ошибка на стороне сервера, попробуйте позднее');
                clearInputs();
            }
        }
    };

    useEffect(() => {
        inputRefs.current[0]?.focus();
    }, []);

    const handleChange = (value: string, index: number) => {
        if (isNaN(Number(value))) return;

        const newOtp = [...otp];
        const char = value.substring(value.length - 1);
        newOtp[index] = char;
        setOtp(newOtp);
        setApiError(null); // Очищаем ошибку при вводе новых данных

        if (char && index < CODE_LENGTH - 1) {
            inputRefs.current[index + 1]?.focus();
        }

        const finalCode = newOtp.join("");
        if (finalCode.length === CODE_LENGTH) {
            submitCode(finalCode);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
        if (e.key === 'Backspace' && !otp[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        } else if (e.key === 'ArrowLeft' && index > 0) {
            inputRefs.current[index - 1]?.focus();
        } else if (e.key === 'ArrowRight' && index < CODE_LENGTH - 1) {
            inputRefs.current[index + 1]?.focus();
        }
    };

    const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
        const data = e.clipboardData.getData("text").trim();
        if (!/^\d{6}$/.test(data)) return;

        const digits = data.split("");
        setOtp(digits);
        setApiError(null);

        inputRefs.current[CODE_LENGTH - 1]?.focus();
        submitCode(data);
    };

    const resendCode = async () => {
        if (timeLeft > 0) return; // Защита от случайного клика

        const success = await authApi.resendCode();
        if (success) {
            setApiError(null);
            clearInputs();
            startTimer(RESEND_DELAY);
        } else {
            setApiError('Ошибка на стороне сервера, попробуйте позже');
        }
    };

    return (
        <div className='confirmation_page__container'>
            <div className="otp-container">
                <h1 className="otp-title">Подтверждение</h1>
                <p className="otp-subtitle">Мы отправили код на вашу почту</p>
                
                <div className="otp-inputs">
                    {otp.map((digit, index) => (
                    <input
                        key={index}
                        type="text"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        ref={(el) => { inputRefs.current[index] = el; }}
                        value={digit}
                        onChange={(e) => handleChange(e.target.value, index)}
                        onKeyDown={(e) => handleKeyDown(e, index)}
                        onPaste={handlePaste}
                        className={apiError ? "otp-input otp-input--error" : "otp-input"}
                    />
                    ))}
                </div>

                <span className='otp-error'>{apiError}</span>
                {timeLeft > 0 ? (
                        <p className='otp-subtitle'>
                            Повторная отправка кода будет доступна через: <b>{timeLeft} сек.</b>
                        </p>
                    ) : (
                        <p className='otp-subtitle'>
                            Не пришел код?{' '}
                            <span className='otp-link' onClick={resendCode}>
                                Отправить еще раз
                            </span>
                        </p>
                    )}
                
            </div>
        </div>
  );
};