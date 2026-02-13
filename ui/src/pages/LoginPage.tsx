import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, Link } from 'react-router-dom'; // Добавил Link
import { authApi } from '../api/AuthApiService';
import { authStore } from '../store/AuthStore';
import './css/LoginPage.css';

// Схема валидации
const LoginSchema = z.object({
  email: z.string().min(1, 'Введите Email').email('Некорректный формат Email'),
  password: z.string().min(6, 'Пароль должен содержать минимум 6 символов')
});

type LoginSchemaType = z.infer<typeof LoginSchema>;

export const LoginPage = () => {
  const [apiError, setApiError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false); // Состояние для глаза
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting }, // Достаем isSubmitting
  } = useForm<LoginSchemaType>({
    resolver: zodResolver(LoginSchema),
    mode: 'onBlur' // Валидация при потере фокуса (лучше для UX)
  });

  const onSubmitHandler = async (data: LoginSchemaType) => {
    setApiError(null);
    try {
      const error = await authApi.login(data);
      
      if (error) {
        setApiError(error.message);
        if (error.errors) {
            // Упрощенная логика обработки полей
            if (error.errors.has('email')) {
                setError('email', { type: 'server', message: error.errors.get('email')?.join(', ') });
            }
            if (error.errors.has('password')) {
                setError('password', { type: 'server', message: error.errors.get('password')?.join(', ') });
            }
        }
      } else {
        const { user } = authStore;
        if (user && !user.isConfirmed) {
          navigate('/account-confirmation');
        } else {
          navigate('/'); // Или куда нужно после логина
        }
      }
    } catch (e) {
      setApiError('Произошла непредвиденная ошибка. Попробуйте позже.');
    }
  };

  return (
    <div className='login-page'>
      <div className='login-card'>
        <div className='login-header'>
          <h1>С возвращением!</h1>
          <p>Введите данные для входа в аккаунт</p>
        </div>

        <form className='login-form' onSubmit={handleSubmit(onSubmitHandler)}>
          {/* Email Field */}
          <div className={`form-group ${errors.email ? 'has-error' : ''}`}>
            <label htmlFor='email'>Email</label>
            <input
              id='email'
              type='email'
              placeholder='name@example.com'
              {...register('email')}
              disabled={isSubmitting}
            />
            {errors.email && <span className='error-message'>{errors.email.message}</span>}
          </div>

          {/* Password Field */}
          <div className={`form-group ${errors.password ? 'has-error' : ''}`}>
            <label htmlFor='password'>Пароль</label>
            <div className='password-input-wrapper'>
              <input
                id='password'
                type={showPassword ? 'text' : 'password'}
                placeholder='Введите пароль'
                {...register('password')}
                disabled={isSubmitting}
              />
              <button
                type='button'
                className='password-toggle'
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1} // Чтобы не фокусироваться табом
              >
                {/* SVG иконка глаза */}
                {showPassword ? (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24M1 1l22 22"/></svg>
                ) : (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                )}
              </button>
            </div>
            {errors.password && <span className='error-message'>{errors.password.message}</span>}
          </div>

          {/* Global API Error */}
          {apiError && (
            <div className='api-error-alert'>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              <span>{apiError}</span>
            </div>
          )}

          {/* Submit Button */}
          <button type='submit' className='submit-btn' disabled={isSubmitting}>
            {isSubmitting ? <span className="loader"></span> : 'Войти'}
          </button>
        </form>

        <div className='login-footer'>
          <span>Нет аккаунта? </span>
          <Link to="/register">Зарегистрироваться</Link>
        </div>
      </div>
    </div>
  );
};