import { useEffect } from 'react';
import './css/HomePage.css'

export const HomePage = () => {
    useEffect(() => {

    }, []);
    return (
        <div className="home_page__container">
            <h1 className="home_page__title">Ваши схемы:</h1>
            <div className="home_page__schema_list">

            </div>
        </div>
    );
}