import './Header.css';
import logo from '@/assets/logo.png';

export const Header = () => {
    return (
        <header>
            <div className={'logo-container'}>
                <img src={logo} alt='erm' />
            </div>
        </header>
    );
}