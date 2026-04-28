import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../api/AuthApiService';
import './css/RegisterPage.css';

// Схема валидации
const RegisterSchema = z.object({
  email: z.string().min(1, 'Введите Email').email('Некорректный формат Email'),
  password: z.string().min(6, 'Пароль должен содержать минимум 6 символов'),
  confirmPassword: z.string().min(1, 'Подтвердите пароль')
}).superRefine(({ password, confirmPassword }, ctx) => {
  if (confirmPassword !== password) {
    ctx.addIssue({
      code: 'custom',
      message: 'Пароли не совпадают',
      path: ['confirmPassword']
    });
  }
});

type RegisterSchemaType = z.infer<typeof RegisterSchema>;

export const RegisterPage = () => {
  const [apiError, setApiError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false); // Один переключатель для обоих полей
  const navigate = useNavigate();

  useEffect(() => {
    document.title = 'Регистрация';
  }, []);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterSchemaType>({
    resolver: zodResolver(RegisterSchema),
    mode: 'onBlur'
  });

  const onSubmitHandler = async (data: RegisterSchemaType) => {
    setApiError(null);
    try {
      const error = await authApi.register(data);
      
      if (error) {
        setApiError(error.message);
        if (error.errors) {
          if (error.errors.has('email')) {
            setError('email', { type: 'server', message: error.errors.get('email')?.join(', ') });
          }
          if (error.errors.has('password')) {
            setError('password', { type: 'server', message: error.errors.get('password')?.join(', ') });
          }
        }
      } else {
        navigate('/account-confirmation');
      }
    } catch (e) {
      setApiError('Произошла ошибка при регистрации. Попробуйте позже.');
    }
  };

  return (
    <div className='register-page'>
      <div className='register-card'>
        <div className='auth-header'>
          <h1>Создать аккаунт</h1>
          <p>Заполните форму для начала работы</p>
        </div>

        <form className='auth-form' onSubmit={handleSubmit(onSubmitHandler)}>
          {/* Email */}
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

          {/* Password */}
          <div className={`form-group ${errors.password ? 'has-error' : ''}`}>
            <label htmlFor='password'>Пароль</label>
            <div className='password-input-wrapper'>
              <input
                id='password'
                type={showPassword ? 'text' : 'password'}
                placeholder='Придумайте пароль'
                {...register('password')}
                disabled={isSubmitting}
              />
              <button
                type='button'
                className='password-toggle'
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
              >
                {showPassword ? (
                   <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24M1 1l22 22"/></svg>
                ) : (
                   <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                )}
              </button>
            </div>
            {errors.password && <span className='error-message'>{errors.password.message}</span>}
          </div>

          {/* Confirm Password */}
          <div className={`form-group ${errors.confirmPassword ? 'has-error' : ''}`}>
            <label htmlFor='confirmPassword'>Повторите пароль</label>
            <input
              id='confirmPassword'
              type={showPassword ? 'text' : 'password'} // Тоже реагирует на глазок
              placeholder='Повторите пароль'
              {...register('confirmPassword')}
              disabled={isSubmitting}
            />
            {errors.confirmPassword && <span className='error-message'>{errors.confirmPassword.message}</span>}
          </div>

          {/* API Error */}
          {apiError && (
            <div className='api-error-alert'>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              <span>{apiError}</span>
            </div>
          )}

          {/* Submit Button */}
          <button type='submit' className='submit-btn' disabled={isSubmitting}>
            {isSubmitting ? <span className="loader"></span> : 'Зарегистрироваться'}
          </button>
        </form>

        <div className='auth-footer'>
          <span>Уже есть аккаунт? </span>
          <Link to="/login">Войти</Link>
        </div>
      </div>
    </div>
  );
};