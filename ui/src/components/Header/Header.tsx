import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import { authStore } from '@/store/AuthStore';
import './Header.css';
import logo from '@/assets/logo.png';
import profilePic from '@/assets/user.png';

export const Header = observer(() => {
    const navigate = useNavigate();
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setIsMenuOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    const handleLogout = () => {
        authStore.logout();
        setIsMenuOpen(false);
    };

    return (
        <header className="header_container">
            <div className="logo__container" onClick={() => navigate('/')}>
                <img id="logo__image" src={logo} alt="erm" />
                <h3 id="logo__text">ERBuilder</h3>
            </div>

            <div className="profile_wrapper" ref={menuRef}>
                <img 
                    className="profile_avatar" 
                    src={profilePic}
                    onClick={() => setIsMenuOpen(!isMenuOpen)}
                />

                {isMenuOpen && (
                    <div className="profile_dropdown">
                        <div className="profile_dropdown_item logout" onClick={handleLogout}>
                            Выйти из аккаунта
                        </div>
                    </div>
                )}
            </div>
        </header>
    );
});