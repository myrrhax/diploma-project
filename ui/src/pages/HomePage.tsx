import { useEffect, useState, type KeyboardEvent } from 'react';
import { observer } from 'mobx-react-lite';
import { useNavigate, useLocation } from 'react-router-dom';
import { schemaStore } from '../store/SchemaStore';
import { schemaApi } from '../api/SchemaApiService';
import profilePic from '@/assets/user.png'; // Оставляем как фоллбэк или дефолт
import { CreateSchemaModal } from '@/components/CreateSchemaModal/CreateSchemaModal';
import { createSchemaModalStore } from '@/store/CreateShemaModalStore';
import { OverlaySpinner } from '@/components/SpinnerLoader/SpinnerLoader';
import './css/HomePage.css';

export const HomePage = observer(() => {
  const location = useLocation();
  const navigate = useNavigate();
  const { schemas, isLoading } = schemaStore;
  const [query, setQuery] = useState('');
  const [authorOnly, setAuthorOnly] = useState(false);

  useEffect(() => {
    schemaApi.loadUserSchemas();
  }, []);

  useEffect(() => {
        if (location.state?.invitationError) {
            alert(location.state.invitationError);
            
            navigate('.', { replace: true, state: {} });
        }
    }, [location, navigate]);

  const handleSearch = async () => {
    await schemaApi.loadUserSchemas(authorOnly, query);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const openSchemaEditor = (id: string) => {
    navigate('/schema/edit/' + id);
  };

  return (
    <div className="home-page">
      <CreateSchemaModal />

      <div className="home-container">
        {/* Header Section */}
        <div className="dashboard-header">
          <div className="title-section">
            <h1>Мои схемы</h1>
            <p className="subtitle">Управляйте своими проектами и схемами</p>
          </div>
          
          {/* Controls: Search & Filter */}
          <div className="controls-section">
            <div className="search-bar">
              <svg className="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              <input 
                type="text" 
                value={query} 
                onChange={(e) => setQuery(e.target.value)} 
                onKeyDown={handleKeyDown}
                placeholder="Поиск по названию..." 
              />
              <button onClick={handleSearch} className="search-btn">Найти</button>
            </div>
            
            <label className="filter-toggle">
              <input 
                type="checkbox" 
                checked={authorOnly} 
                onChange={(e) => setAuthorOnly(e.target.checked)} 
              />
              <span className="toggle-slider"></span>
              <span className="toggle-label">Только мои</span>
            </label>
          </div>
        </div>
        <div className='btn_create_holder'>
            <button 
              onClick={() => createSchemaModalStore.openCreateModal()}
              className='btn_create'
            >
              Создать схему
          </button>
        </div>
        

        {/* Content Section */}
        {isLoading ? (
          <OverlaySpinner text='Загрузка...' />
        ) : schemas && schemas.length > 0 ? (
          <div className="schema-grid">
            {schemas.map((schema) => (
              <div 
                key={schema.id} 
                className="schema-card" 
                onClick={() => openSchemaEditor(schema.id)}
              >
                <div className="card-preview">
                  {/* Плейсхолдер для превью схемы. Можно заменить на скриншот если есть */}
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="schema-icon"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><line x1="3" y1="9" x2="21" y2="9"></line><line x1="9" y1="21" x2="9" y2="9"></line></svg>
                </div>
                <div className="card-content">
                  <h3 className="schema-name">{schema.name}</h3>
                  <div className="schema-meta">
                    <div className="author-info">
                       <img src={profilePic} alt="User" className="avatar-mini" />
                       <span className="author-email" title={schema.creator.email}>
                         {schema.creator.email}
                       </span>
                    </div>
                    {/* Можно добавить дату создания, если есть в API */}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-icon">📂</div>
            <h3>Здесь пока пусто</h3>
            <p>Вы не создали ни одной схемы или ничего не найдено по запросу.</p>
          </div>
        )}
      </div>
    </div>
  );
});