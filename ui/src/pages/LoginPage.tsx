import z from 'zod'
import './css/LoginPage.css'
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

const LoginSchema = z.object({
    email: z.email('Введите Email'),
    password: z.string().min(6, 'Пароль должен содержать более 5 символов')
});
type LoginSchemaType = z.infer<typeof LoginSchema>;

export const LoginPage = () => {
    const onSubmitHadnler = (data: LoginSchemaType) => {
        console.log("data: " + data)
    }
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm({
        resolver: zodResolver(LoginSchema)
    })

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
                <div className='form_row__container'>
                    <button type='submit'>Войти</button>
                </div>
            </form>
        </div>
    )
}