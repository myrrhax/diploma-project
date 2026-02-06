import z from 'zod'
import './css/RegisterPage.css'
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { authApi } from '../api/AuthApiService';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const RegisterSchema = z.object({
    email: z.email('Введите Email'),
    password: z.string().min(6, 'Пароль должен содержать более 5 символов'),
    confirmPassword: z.string()
}).superRefine(({password, confirmPassword}, ctx) => {
    if (confirmPassword !== password) {
        ctx.addIssue({
            code: 'custom',
            message: 'Пароли не совпадают',
            path: ['confirmPassword']
        })
    }
});
type RegisterSchemaType = z.infer<typeof RegisterSchema>;

export const RegisterPage = () => {
    const [apiError, setApiError] = useState<string | null>();
    const navigate = useNavigate();

    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm({
        resolver: zodResolver(RegisterSchema)
    })

    const onSubmitHadnler = async (data: RegisterSchemaType) => {
        const error = await authApi.register(data);
        if (error) {
            setApiError(error.message);
            if (error.errors) {
                const apiErrors = error.errors;
                if (apiErrors.has('email')) {
                    setError('email', {
                        type: 'server',
                        message: apiErrors.get('email')?.join(',')
                    })
                }
                if (apiErrors.has('password')) {
                    setError('password', {
                        type: 'server',
                        message: apiErrors.get('password')?.join(',')
                    })
                }
            }
        } else {
            navigate('/account-confirmation');
        }
    }

    return (
        <div className='register_page__container'>
            <form className='register_page__form' onSubmit={handleSubmit(onSubmitHadnler)}>
                <div className='form__title_container'>
                    <h1>Регистрация</h1>
                </div>
                <div className='form_row__container'>
                    <label className='form_row__label'>Email</label>
                    <input type='email' className='form_row__input' {...register('email')} />
                    { errors.email && <span className='form_row__error'>{errors.email.message}</span> }
                </div>
                <div className='form_row__container'>
                    <label className='form_row__label'>Пароль</label>
                    <input type='password' className='form_row__input' {...register('password')} />
                    { errors.password && <span className='form_row__error'>{errors.password.message}</span> }
                </div>
                <div className='form_row__container'>
                    <label className='form_row__label'>Подтвердите пароль</label>
                    <input type='password' className='form_row__input' {...register('confirmPassword')} />
                    { errors.confirmPassword && <span className='form_row__error'>{errors.confirmPassword.message}</span> }
                </div>
                { apiError && <span className='form_row__error'>{apiError}</span> }
                <div className='form_row__container'>
                    <button type='submit'>Создать аккаунт</button>
                </div>
            </form>
        </div>
    )
}