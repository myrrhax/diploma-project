import { useEffect } from 'react';
import './css/HomePage.css';
import { schemaStore } from '../store/SchemaStore';
import profilePic from '@/assets/user.png';

export const HomePage = () => {
    useEffect(() => {
        
    }, []);

    return (
        <div className="home_page__container">
            <h1 className="home_page__title">Ваши схемы:</h1>

            {schemaStore.isLoading ? (
                <div className="loader">Загрузка...</div>
            ) : schemaStore.schemas && schemaStore.schemas.length > 0 ? (
                <div className="home_page__schema_list">
                    {schemaStore.schemas.map((schema, _) => (
                        <div className='home_page__schema_element'>
                            <div className='schema_element__name'>{ schema.name }</div>
                            <div className='schema_element__creator'> 
                                <img className='schema_element__creator__pic' src={profilePic} />
                                <span className='schema_element__creator__name'>{ schema.creator.email }</span>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="home_page__no_elements">
                    Здесь пока ничего нет
                </div>
            )}
        </div>
    );
};
