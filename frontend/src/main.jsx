import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './index.css'
import App from './App.jsx'

// Safari en el iPhone ignora user-scalable=no: el pellizco se frena acá. Solo en pantallas táctiles, para no
// bloquear el zoom con el trackpad en Safari de la Mac.
if (navigator.maxTouchPoints > 0) {
  document.addEventListener('gesturestart', (e) => e.preventDefault())
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
