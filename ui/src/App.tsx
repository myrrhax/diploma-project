import { Header } from './components/Header/Header'
import './App.css'
import { Router } from 'react-router-dom';
import { AppRouter } from './router/Router';

function App() {
  return (
    <>
      <AppRouter />
    </>
  )
}

export default App;