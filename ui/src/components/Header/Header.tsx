import './Header.css';
import logo from '@/assets/logo.png';
import { useNavigate } from 'react-router-dom';

export const Header = () => {
    const navigate = useNavigate();

    return (
        <header>
            <div className={'logo__container'} onClick={() => navigate('/')}>
                <img id='logo__image' src={logo} alt='erm' />
                <h3 id='logo__text'>ERM DEV</h3>
            </div>
        </header>
    );
}