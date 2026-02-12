import { useEffect, useState } from 'react';
import './css/HomePage.css';
import { schemaStore } from '../store/SchemaStore';
import profilePic from '@/assets/user.png';
import searchPic from '@/assets/search.png'
import { schemaApi } from '../api/SchemaApiService';
import { observer } from 'mobx-react-lite';
import { useNavigate } from 'react-router-dom';

export const HomePage = observer(() => {
    const navigate = useNavigate();
    const { schemas, isLoading } = schemaStore;
    const [query, setQuery] = useState('');
    const [authorOnly, setAuthorOnly] = useState(false);
    
    useEffect(() => {
        schemaApi.loadUserSchemas();
    }, []);

    const onSearchClick = async () => {
        await schemaApi.loadUserSchemas(authorOnly, query);
    }

    const openSchemaEditor = (id: string) => {
        navigate('/schema/edit/' + id);
    };

    return (
        <div className="home_page__container">
            <div className='home_page__header'>
                <h1 className="home_page__title">Ваши схемы:</h1>
                <div className='home_page__schema_control'>
                    <div className='schema_input'>
                        <input type='text' 
                            value={query} onChange={(e) => setQuery(e.target.value)} placeholder='Имя схемы' />
                        <img src={searchPic} onClick={onSearchClick} />
                    </div>
                    <div className='schema_search_mine'>
                        <span>Только авторские</span>
                        <input type='checkbox' checked={authorOnly} onChange={(e) => setAuthorOnly(e.target.checked)}  />
                    </div>
                </div>
            </div>
            
            {isLoading ? (
                <div className="loader">Загрузка...</div>
            ) : schemas && schemas.length > 0 ? (
                <div className="home_page__schema_list">
                    {schemas.map((schema, _) => (
                        <div className='home_page__schema_element' onClick={() => openSchemaEditor(schema.id)}>
                            <div className='schema_element__name'>{ schema.name }</div>
                            <div className='schema_element__creator'> 
                                <span className='author_label'>Автор: </span>
                                <img src={profilePic} />
                                <span className='author_name'>{ schema.creator.email }</span>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="home_page__no_elements">
                    Здесь пока ничего нет, вы можете добавить схему или присоединиться по пригласительной ссылке
                </div>
            )}
        </div>
    );
});
