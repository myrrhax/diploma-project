import React, { useState, useRef, useEffect } from 'react';
import { z } from 'zod';
import './css/ConfirmationPage.css'
import { authApi } from '../api/AuthApiService';

const otpSchema = z.string().length(6).regex(/^\d+$/);

export const AccountConfirmationPage = () => {
  const [otp, setOtp] = useState<string[]>(new Array(6).fill(""));
  const [apiError, setApiError] = useState<string | null>();
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const submitCode = async (code: string) => {
    const result = otpSchema.safeParse(code);
    if (result.success) {
        try {
            console.log("Код отправлен автоматически:", code);
            const error = await authApi.confirmEmail(code);
            if (error) {
                console.error('Failed to confirm email');
                setApiError(error.message);
            }
        } catch (e: any) {
            setApiError('Ошибка на стороне сервера, попробуйте позднее');
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

    if (char && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }

    const finalCode = newOtp.join("");
    if (finalCode.length === 6) {
      submitCode(finalCode);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    const data = e.clipboardData.getData("text").trim();
    if (!/^\d{6}$/.test(data)) return;

    const digits = data.split("");
    setOtp(digits);
    
    // Фокус на последний элемент и отправка
    inputRefs.current[5]?.focus();
    submitCode(data);
  };

  const resendCode = async () => {

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
                    className="otp-input"
                />
                ))}
            </div>

            <span className='otp-error'>{apiError}</span>
            <p className='otp-subtitle'>Не пришел код? <span className='otp-link' onClick={() => resendCode()}>Отправить еще раз</span></p>
        </div>
    </div>
  );
};