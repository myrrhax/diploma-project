import z from 'zod'
import './css/LoginPage.css'
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { authApi } from '../api/AuthApiService';
import { useState } from 'react';

const LoginSchema = z.object({
    email: z.email('Введите Email'),
    password: z.string().min(6, 'Пароль должен содержать более 5 символов')
});
type LoginSchemaType = z.infer<typeof LoginSchema>;

export const LoginPage = () => {
    const [apiError, setApiError] = useState<string | null>();

    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm({
        resolver: zodResolver(LoginSchema)
    })

    const onSubmitHadnler = async (data: LoginSchemaType) => {
        const error = await authApi.login(data);
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

        }
    }

    return (
        <div className='login_page__container'>
            <form className='login_page__form' onSubmit={handleSubmit(onSubmitHadnler)}>
                <div className='form__title_container'>
                    <h1>Вход</h1>
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
                { apiError && <span className='form_row__error'>{apiError}</span> }
                <div className='form_row__container'>
                    <button type='submit'>Войти</button>
                </div>
            </form>
        </div>
    )
}